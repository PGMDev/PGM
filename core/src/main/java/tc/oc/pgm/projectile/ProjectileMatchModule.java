package tc.oc.pgm.projectile;

import static tc.oc.pgm.util.Assert.assertTrue;

import com.google.common.collect.ImmutableSet;
import java.util.HashMap;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Explosive;
import org.bukkit.entity.FallingBlock;
import org.bukkit.entity.Fireball;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerPickupItemEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.util.BlockIterator;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.Nullable;
import tc.oc.pgm.api.PGM;
import tc.oc.pgm.api.event.BlockTransformEvent;
import tc.oc.pgm.api.filter.Filter;
import tc.oc.pgm.api.filter.query.Query;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.match.MatchModule;
import tc.oc.pgm.api.match.MatchScope;
import tc.oc.pgm.api.party.Party;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.api.player.ParticipantState;
import tc.oc.pgm.events.ListenerScope;
import tc.oc.pgm.events.PlayerParticipationStopEvent;
import tc.oc.pgm.filters.query.BlockQuery;
import tc.oc.pgm.filters.query.PlayerBlockQuery;
import tc.oc.pgm.kits.tag.ItemTags;
import tc.oc.pgm.util.MatchPlayers;
import tc.oc.pgm.util.TimeUtils;
import tc.oc.pgm.util.bukkit.MetadataUtils;
import tc.oc.pgm.util.bukkit.entities.BlockEntity;
import tc.oc.pgm.util.inventory.InventoryUtils;
import tc.oc.pgm.util.nms.NMSHacks;

@ListenerScope(MatchScope.RUNNING)
public class ProjectileMatchModule implements MatchModule, Listener {

  // Holds the definition for a projectile from immediately before launch to
  // just after the definition is attached as metadata. Bukkit fires various
  // creation events before we have a chance to attach the metadata, and this
  // is how we make the definition available from within those events. The
  // ThreadLocal is not necessary, but it doesn't hurt and it's good form.
  private static final ThreadLocal<ProjectileDefinition> launchingDefinition = new ThreadLocal<>();

  private final Match match;
  private final ImmutableSet<ProjectileDefinition> projectileDefinitions;
  private final HashMap<UUID, ProjectileCooldowns> projectileCooldowns = new HashMap<>();

  private static final String DEFINITION_KEY = "projectileDefinition";

  public ProjectileMatchModule(
      Match match, ImmutableSet<ProjectileDefinition> projectileDefinitions) {
    this.match = match;
    this.projectileDefinitions = projectileDefinitions;
  }

  @EventHandler
  public void onClickEvent(PlayerInteractEvent event) {
    if (event.getAction() == Action.PHYSICAL) return;

    Player player = event.getPlayer();
    ParticipantState playerState = match.getParticipantState(player);
    if (playerState == null) return;

    var definition = this.getProjectileDefinition(player.getItemInHand());

    if (definition == null || !isValidProjectileAction(event.getAction(), definition.clickAction))
      return;

    // Prevent the original projectile from being fired
    event.setCancelled(true);

    if (this.isCooldownActive(player, definition)) return;

    var projType = definition.projectile;
    boolean needsEvent = true;
    Vector velocity = player.getEyeLocation().getDirection().multiply(definition.velocity);
    Entity projectile = null;
    try {
      assertTrue(launchingDefinition.get() == null, "nested projectile launch");
      launchingDefinition.set(definition);
      switch (projType) {
        case ProjectileDefinition.RealEntity(Class<? extends Entity> entityType) -> {
          if (Projectile.class.isAssignableFrom(entityType)) {
            needsEvent = false;

            projectile = player.launchProjectile(entityType.asSubclass(Projectile.class), velocity);
            if (projectile instanceof Fireball fireball && definition.precise) {
              NMSHacks.NMS_HACKS.setFireballDirection(fireball, velocity);
            }
          } else {
            if (FallingBlock.class.isAssignableFrom(entityType)) {
              projectile = definition.blockMaterial.spawnFallingBlock(player.getEyeLocation());
            } else {
              projectile = player.getWorld().spawn(player.getEyeLocation(), entityType);
            }
            projectile.setVelocity(velocity);
          }
        }
        case ProjectileDefinition.BlockEntityType ce -> {
          var be = BlockEntity.spawnBlockEntity(
              player.getEyeLocation(), definition.blockMaterial, ce.size(), velocity);
          new BlockRunner(definition, be, player, player.getEyeLocation());
        }
      }

      if (definition.power != null && projectile instanceof Explosive) {
        ((Explosive) projectile).setYield(definition.power);
      }
      if (projectile != null) {
        projectile.setMetadata(
            "projectileDefinition", new FixedMetadataValue(PGM.get(), definition));
      }
    } finally {
      launchingDefinition.remove();
    }

    // If the entity implements Projectile, it will have already generated a
    // ProjectileLaunchEvent.
    // Otherwise, we fire our custom event.
    if (needsEvent && projectile != null) {
      EntityLaunchEvent launchEvent = new EntityLaunchEvent(projectile, event.getPlayer());
      match.callEvent(launchEvent);
      if (launchEvent.isCancelled()) {
        projectile.remove();
        return;
      }
    }

    if (definition.throwable) {
      InventoryUtils.consumeItem(event);
    }

    if (definition.coolDown != null) {
      startCooldown(player, definition);
    }
  }

  @EventHandler
  public void onProjectileHurtEvent(EntityDamageByEntityEvent event) {
    if (!(event.getEntity() instanceof LivingEntity)) return;
    LivingEntity damagedEntity = (LivingEntity) event.getEntity();

    ProjectileDefinition projectileDefinition =
        ProjectileMatchModule.getProjectileDefinition(event.getDamager());

    if (projectileDefinition != null) {
      if (!projectileDefinition.potion.isEmpty()) {
        damagedEntity.addPotionEffects(projectileDefinition.potion);
      }

      if (projectileDefinition.damage != null) {
        event.setDamage(projectileDefinition.damage);
      }
    }
  }

  @EventHandler
  public void onProjectileHitEvent(ProjectileHitEvent event) {
    Projectile projectile = event.getEntity();
    ProjectileDefinition projectileDefinition = getProjectileDefinition(projectile);
    if (projectileDefinition == null) return;
    Filter filter = projectileDefinition.destroyFilter;
    if (filter == null) return;

    BlockIterator it = new BlockIterator(
        projectile.getWorld(),
        projectile.getLocation().toVector(),
        projectile.getVelocity().normalize(),
        0d,
        2);

    Block hitBlock = null;
    while (it.hasNext()) {
      hitBlock = it.next();

      if (hitBlock.getType() != Material.AIR) {
        break;
      }
    }

    if (hitBlock != null) {
      MatchPlayer player = projectile.getShooter() instanceof Player
          ? match.getPlayer((Player) projectile.getShooter())
          : null;
      Query query = player != null
          ? new PlayerBlockQuery(event, player, hitBlock.getState())
          : new BlockQuery(event, hitBlock);

      if (filter.query(query).isAllowed()) {
        BlockTransformEvent bte = new BlockTransformEvent(event, hitBlock, Material.AIR);
        match.callEvent(bte);
        if (!bte.isCancelled()) {
          hitBlock.setType(Material.AIR);
          projectile.remove();
        }
      }
    }
  }

  @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
  public void onProjectileDropEvent(PlayerDropItemEvent event) {
    this.resetItemName(event.getItemDrop().getItemStack());
  }

  @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
  public void onPlayerDeathEvent(PlayerDeathEvent event) {
    for (ItemStack itemStack : event.getDrops()) {
      this.resetItemName(itemStack);
    }
  }

  @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
  public void onPlayerPickupProjectileEvent(PlayerPickupItemEvent event) {
    ItemStack itemStack = event.getItem().getItemStack();
    ProjectileDefinition definition = getProjectileDefinition(itemStack);
    ProjectileCooldowns cooldowns = projectileCooldowns.get(event.getPlayer().getUniqueId());

    if (cooldowns != null && cooldowns.isActive(definition)) {
      cooldowns.setItemCountdownName(itemStack, definition);
    }
  }

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onParticipationStop(PlayerParticipationStopEvent event) {
    projectileCooldowns.remove(event.getPlayer().getId());
  }

  public void resetItemName(ItemStack item) {
    ProjectileDefinition projectileDefinition = getProjectileDefinition(item);
    if (projectileDefinition != null) {
      ItemMeta itemMeta = item.getItemMeta();
      itemMeta.setDisplayName(ItemTags.ORIGINAL_NAME.get(item));
      item.setItemMeta(itemMeta);
    }
  }

  public ProjectileDefinition getProjectileDefinition(ItemStack item) {
    return getProjectileDefinition(ItemTags.PROJECTILE.get(item));
  }

  public ProjectileDefinition getProjectileDefinition(String projectileId) {
    if (projectileId == null) return null;
    for (ProjectileDefinition projectileDefinition : projectileDefinitions) {
      if (projectileId.equals(projectileDefinition.getId())) {
        return projectileDefinition;
      }
    }

    return null;
  }

  public static @Nullable ProjectileDefinition getProjectileDefinition(Entity entity) {
    if (entity.hasMetadata(DEFINITION_KEY)) {
      return MetadataUtils.getMetadataValue(entity, DEFINITION_KEY, PGM.get());
    }
    return launchingDefinition.get();
  }

  private static boolean isValidProjectileAction(Action action, ClickAction clickAction) {
    switch (clickAction) {
      case RIGHT:
        return action == Action.RIGHT_CLICK_AIR || action == Action.RIGHT_CLICK_BLOCK;
      case LEFT:
        return action == Action.LEFT_CLICK_AIR || action == Action.LEFT_CLICK_BLOCK;
      case BOTH:
        return action != Action.PHYSICAL;
    }
    return false;
  }

  private void startCooldown(Player player, ProjectileDefinition definition) {
    ProjectileCooldowns playerCooldowns = projectileCooldowns.get(player.getUniqueId());
    if (playerCooldowns == null) {
      playerCooldowns = new ProjectileCooldowns(this, match.getPlayer(player));
      projectileCooldowns.put(player.getUniqueId(), playerCooldowns);
    }

    playerCooldowns.start(definition);
  }

  public boolean isCooldownActive(Player player, ProjectileDefinition definition) {
    ProjectileCooldowns playerCooldowns = projectileCooldowns.get(player.getUniqueId());
    return (playerCooldowns != null && playerCooldowns.isActive(definition));
  }

  private class BlockRunner {
    private final ProjectileDefinition definition;
    private final BlockEntity blockEntity;
    private final Player player;
    private final Party shooterParty;
    private final ProjectileDefinition.BlockEntityType ce;
    private final Location currentLocation;
    private final Vector increment;
    private final Vector substep;
    private final int substeps;
    private final Future<?> runner;
    private int remainingTime;

    public BlockRunner(
        ProjectileDefinition definition,
        BlockEntity blockEntity,
        Player player,
        Location spawnLocation) {
      this.definition = definition;
      this.blockEntity = blockEntity;
      this.player = player;
      this.shooterParty = Objects.requireNonNull(match.getPlayer(player)).getParty();
      this.ce = (ProjectileDefinition.BlockEntityType) definition.projectile;

      this.currentLocation = spawnLocation;

      var normalizedDirection = currentLocation.getDirection().normalize();
      this.increment = normalizedDirection.multiply(definition.velocity);
      this.substeps = Math.max(1, (int) (definition.velocity / ce.size()));
      this.substep = increment.clone().divide(new Vector(substeps, substeps, substeps));

      this.remainingTime = (int) TimeUtils.toTicks(ce.maxTravelTime());
      this.runner = match
          .getExecutor(MatchScope.RUNNING)
          .scheduleAtFixedRate(this::tick, 0L, 50L, TimeUnit.MILLISECONDS);
    }

    public void tick() {
      if (remainingTime-- <= 0) {
        cancel();
        return;
      }
      if (definition.damage != null || ce.solidBlockCollision()) {
        Location substepLoc = currentLocation.clone();
        for (int i = substeps; i > 0; i--) {
          substepLoc.add(substep);
          if (!blockDisplayCollision(substepLoc)) continue;
          cancel();
          return;
        }
      }

      currentLocation.add(increment);
      blockEntity.teleport(currentLocation);
    }

    private void cancel() {
      this.runner.cancel(true);
      blockEntity.remove();
    }

    private boolean blockDisplayCollision(Location location) {
      double radius = 0.5 * ce.size();
      // Make location be center of the entity instead of feet
      location.setY(location.getY() + radius);

      if (definition.damage != null) {
        for (Player victim : location.getNearbyPlayers(0.5 * ce.size())) {
          var mpVictim = match.getPlayer(victim);
          if (MatchPlayers.canInteract(mpVictim) && mpVictim.getParty() != shooterParty) {
            victim.damage(definition.damage, player);
            return true;
          }
        }
      }
      if (ce.solidBlockCollision()) {

        int x1 = (int) Math.floor(location.getX() - radius);
        int y1 = (int) Math.floor(location.getY());
        int z1 = (int) Math.floor(location.getZ() - radius);

        int x2 = (int) Math.floor(location.getX() + radius);
        int y2 = (int) Math.floor(location.getY() + ce.size());
        int z2 = (int) Math.floor(location.getZ() + radius);

        Location loc = location.clone();

        for (int x = x1; x <= x2; ++x) {
          loc.setX(x);
          for (int y = y1; y <= y2; ++y) {
            loc.setY(y);
            for (int z = z1; z <= z2; ++z) {
              loc.setZ(z);
              if (loc.getBlock().getType().isSolid()) return true;
            }
          }
        }
      }

      // revert back to normal
      location.setY(location.getY() - radius);
      return false;
    }
  }
}
