package tc.oc.pgm.projectile;

import static tc.oc.pgm.util.Assert.assertTrue;

import com.google.common.collect.ImmutableSet;
import java.util.HashMap;
import java.util.UUID;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Explosive;
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
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.api.player.ParticipantState;
import tc.oc.pgm.events.ListenerScope;
import tc.oc.pgm.events.PlayerParticipationStopEvent;
import tc.oc.pgm.filters.query.BlockQuery;
import tc.oc.pgm.filters.query.PlayerBlockQuery;
import tc.oc.pgm.kits.tag.ItemTags;
import tc.oc.pgm.util.bukkit.MetadataUtils;
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

    ProjectileDefinition projectileDefinition =
        this.getProjectileDefinition(player.getItemInHand());

    if (projectileDefinition != null
        && isValidProjectileAction(event.getAction(), projectileDefinition.clickAction)) {
      // Prevent the original projectile from being fired
      event.setCancelled(true);

      if (this.isCooldownActive(player, projectileDefinition)) return;

      boolean realProjectile = Projectile.class.isAssignableFrom(projectileDefinition.projectile);
      Vector velocity =
          player.getEyeLocation().getDirection().multiply(projectileDefinition.velocity);
      Entity projectile;
      try {
        assertTrue(launchingDefinition.get() == null, "nested projectile launch");
        launchingDefinition.set(projectileDefinition);
        if (realProjectile) {
          projectile =
              player.launchProjectile(
                  projectileDefinition.projectile.asSubclass(Projectile.class), velocity);
          if (projectile instanceof Fireball && projectileDefinition.precise) {
            NMSHacks.NMS_HACKS.setFireballDirection((Fireball) projectile, velocity);
          }
        } else {
          projectile =
              player.getWorld().spawn(player.getEyeLocation(), projectileDefinition.projectile);
          projectile.setVelocity(velocity);
        }
        if (projectileDefinition.power != null && projectile instanceof Explosive) {
          ((Explosive) projectile).setYield(projectileDefinition.power);
        }
        projectile.setMetadata(
            "projectileDefinition", new FixedMetadataValue(PGM.get(), projectileDefinition));
      } finally {
        launchingDefinition.remove();
      }

      // If the entity implements Projectile, it will have already generated a
      // ProjectileLaunchEvent.
      // Otherwise, we fire our custom event.
      if (!realProjectile) {
        EntityLaunchEvent launchEvent = new EntityLaunchEvent(projectile, event.getPlayer());
        match.callEvent(launchEvent);
        if (launchEvent.isCancelled()) {
          projectile.remove();
          return;
        }
      }

      if (projectileDefinition.throwable) {
        InventoryUtils.consumeItem(player);
      }

      if (projectileDefinition.coolDown != null) {
        startCooldown(player, projectileDefinition);
      }
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

    BlockIterator it =
        new BlockIterator(
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
      MatchPlayer player =
          projectile.getShooter() instanceof Player
              ? match.getPlayer((Player) projectile.getShooter())
              : null;
      Query query =
          player != null
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
}
