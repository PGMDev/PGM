package tc.oc.pgm.projectile;

import static tc.oc.pgm.util.Assert.assertTrue;
import static tc.oc.pgm.util.nms.Entities.CUSTOM_ENTITIES;

import com.google.common.collect.ImmutableSet;

import java.util.Collection;
import java.util.HashMap;
import java.util.UUID;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.BooleanSupplier;

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
import tc.oc.pgm.projectile.path.LinearProjectilePath;
import tc.oc.pgm.projectile.path.ProjectilePath;
import tc.oc.pgm.util.bukkit.MetadataUtils;
import tc.oc.pgm.util.inventory.InventoryUtils;
import tc.oc.pgm.util.nms.NMSHacks;
import tc.oc.pgm.util.nms.entities.BlockEntity;

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

      var projType = projectileDefinition.projectile;
      boolean realProjectile = projType instanceof ProjectileDefinition.ProjectileEntity.RealEntity re
              && Projectile.class.isAssignableFrom(re.entityType());
      if (projectileDefinition.projectile instanceof ProjectileDefinition.ProjectileEntity.RealEntity) {
        realProjectile = Projectile.class.isAssignableFrom(
            ((ProjectileDefinition.ProjectileEntity.RealEntity) projectileDefinition.projectile).entityType()
        );
      }
      Vector velocity =
          player.getEyeLocation().getDirection().multiply(projectileDefinition.velocity);
      Entity projectile = null;
      BlockEntity blockEntity = null;
      try {
        assertTrue(launchingDefinition.get() == null, "nested projectile launch");
        launchingDefinition.set(projectileDefinition);
        if (realProjectile) {
          projectile = player.launchProjectile(
            (((ProjectileDefinition.ProjectileEntity.RealEntity) projectileDefinition.projectile).entityType())
              .asSubclass(Projectile.class),
              velocity
          );
          if (projectile instanceof Fireball fireball && projectileDefinition.precise) {
            NMSHacks.NMS_HACKS.setFireballDirection(fireball, velocity);
          }
        } else {
          if ((projectileDefinition.projectile instanceof ProjectileDefinition.ProjectileEntity.RealEntity) &&
                  FallingBlock.class.isAssignableFrom((((ProjectileDefinition.ProjectileEntity.RealEntity) projectileDefinition.projectile).entityType()))) {
            projectile =
                projectileDefinition.blockMaterial.spawnFallingBlock(player.getEyeLocation());
          } else {
            Location loc = player.getEyeLocation();
            if ((projectileDefinition.projectile instanceof ProjectileDefinition.ProjectileEntity.CustomEntity ce) &&
                    ce.entityType() == ProjectileDefinition.ProjectileEntity.CustomEntityType.BLOCK) {
              loc.setPitch(0);
              loc.setYaw(0);
              blockEntity = CUSTOM_ENTITIES.spawnBlockEntity(loc, projectileDefinition.blockMaterial, ce.size(), velocity);
            } else if (projectileDefinition.projectile instanceof ProjectileDefinition.ProjectileEntity.RealEntity re) {
              projectile =
                      player.getWorld().spawn(loc, re.entityType());
            }
          }
          if (projectile != null) {
            projectile.setVelocity(velocity);
            projectile.setMetadata(
                    "projectileDefinition", new FixedMetadataValue(PGM.get(), projectileDefinition));
          }
        }
        if (projectileDefinition.power != null && projectile instanceof Explosive) {
          ((Explosive) projectile).setYield(projectileDefinition.power);
        }
        if (blockEntity != null && blockEntity.isDisplayEntity()) {
          final Vector normalizedDirection = player.getLocation().getDirection().normalize();
          final LinearProjectilePath linearProjectilePath = new LinearProjectilePath(
            normalizedDirection, projectileDefinition.velocity
          );
          final Location initialLocation = blockEntity.getLocation();

          BlockEntity finalBlockEntity = blockEntity;
          ProjectileDefinition.ProjectileEntity.CustomEntity ce =
                  (ProjectileDefinition.ProjectileEntity.CustomEntity) projectileDefinition.projectile;
          runFixedTimesAtPeriod(
              match.getExecutor(MatchScope.RUNNING),
              new BooleanSupplier() {
                private int progress = 0;

                @Override
                public boolean getAsBoolean() {
                  finalBlockEntity.setTeleportationDuration(1);

                  Location currentLocation = finalBlockEntity.getLocation();
                  Location incrementingLocation = currentLocation.clone();
                  Location newLocation = calculateTo(initialLocation, linearProjectilePath, ++progress);

                  finalBlockEntity.teleport(newLocation);
                  if (projectileDefinition.damage != null || ce.solidBlockCollision()) {
                    while (currentLocation.distanceSquared(incrementingLocation) < currentLocation.distanceSquared(newLocation)) {
                      if (blockDisplayCollision(projectileDefinition, ce, player, incrementingLocation)) {
                        return true;
                      }
                      incrementingLocation.add(normalizedDirection.clone().multiply(ce.size()));
                    }
                    return blockDisplayCollision(projectileDefinition, ce, player, incrementingLocation);
                  }
                  return false;
                }
              },
              1L, ce.maxTravelTime().toMillis(), blockEntity::remove
          );
        }
      } finally {
        launchingDefinition.remove();
      }

      // If the entity implements Projectile, it will have already generated a
      // ProjectileLaunchEvent.
      // Otherwise, we fire our custom event.
      if (!realProjectile && projectile != null) {
        EntityLaunchEvent launchEvent = new EntityLaunchEvent(projectile, event.getPlayer());
        match.callEvent(launchEvent);
        if (launchEvent.isCancelled()) {
          projectile.remove();
          return;
        }
      }

      if (projectileDefinition.throwable) {
        InventoryUtils.consumeItem(event);
      }

      if (projectileDefinition.coolDown != null) {
        startCooldown(player, projectileDefinition);
      }
    }
  }

  private boolean blockDisplayCollision(ProjectileDefinition projectileDefinition, ProjectileDefinition.ProjectileEntity.CustomEntity ce, Player player, Location location) {
    if (projectileDefinition.damage != null) {
      Collection<Entity> nearbyEntities = location.getNearbyEntities(0.5 * ce.size(), 0.5 * ce.size(), 0.5 * ce.size());
      if (!nearbyEntities.isEmpty()) {
        Party playerParty = PGM.get().getMatchManager().getPlayer(player).getParty();
        for (Entity entity : nearbyEntities) {
          if (entity instanceof Player) {
            if (playerParty != PGM.get().getMatchManager().getPlayer(((Player) entity)).getParty()) {
              ((Player) entity).damage(projectileDefinition.damage, player);
              return true;
            }
          }
        }
      }
    }
    if (ce.solidBlockCollision()) {
      double posScale = 0.5 * ce.size();
      double negScale = -0.5 * ce.size();

      Block b1 = location.clone().add(negScale, negScale, negScale).getBlock();
      Block b2 = location.clone().add(posScale, negScale, negScale).getBlock();
      Block b3 = location.clone().add(negScale, posScale, negScale).getBlock();
      Block b4 = location.clone().add(posScale, posScale, negScale).getBlock();
      Block b5 = location.clone().add(negScale, negScale, posScale).getBlock();
      Block b6 = location.clone().add(posScale, negScale, posScale).getBlock();
      Block b7 = location.clone().add(negScale, posScale, posScale).getBlock();
      Block b8 = location.clone().add(posScale, posScale, posScale).getBlock();

      return b1.getType().isSolid() || b2.getType().isSolid() || b3.getType().isSolid() || b4.getType().isSolid() || b5.getType().isSolid() || b6.getType().isSolid() || b7.getType().isSolid() || b8.getType().isSolid();
    }
    return false;
  }

  // For entities which need their velocity simulated
  private Location calculateTo(
          final Location entityLocation,
          final ProjectilePath path,
          final int progress) {
    return entityLocation.clone().add(path.getPositionAtProgress(progress));
  }

  private static void runFixedTimesAtPeriod(
    final ScheduledExecutorService scheduledExecutorService, final BooleanSupplier runnable,
    final long periodTicks, final long upperBoundMillis,
    final Runnable finishHandler
  ) {
    final ScheduledFuture<?>[] ref = new ScheduledFuture[2];
    final ScheduledFuture<?> mainTask = scheduledExecutorService.scheduleAtFixedRate(
        () -> {
          final boolean shouldCancel = runnable.getAsBoolean();
          if (shouldCancel) {
            ref[0].cancel(true);
            ref[1].cancel(true);
            finishHandler.run();
          }
        }, 0L, periodTicks * 50L, TimeUnit.MILLISECONDS
    );
    final ScheduledFuture<?> cleanupTask = scheduledExecutorService.schedule(() -> {
      if (ref[0].isCancelled()) return;
      ref[0].cancel(true);
      finishHandler.run();
    }, upperBoundMillis, TimeUnit.MILLISECONDS);
    ref[0] = mainTask;
    ref[1] = cleanupTask;
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
}
