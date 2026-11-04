package tc.oc.pgm.blockdrops;

import static tc.oc.pgm.util.bukkit.Effects.EFFECTS;
import static tc.oc.pgm.util.bukkit.MiscUtils.MISC_UTILS;
import static tc.oc.pgm.util.material.Materials.DOORS;

import java.util.HashSet;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.ExperienceOrb;
import org.bukkit.entity.FallingBlock;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.entity.EntityChangeBlockEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;
import org.jspecify.annotations.Nullable;
import tc.oc.pgm.api.event.BlockTransformEvent;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.match.MatchModule;
import tc.oc.pgm.api.match.MatchScope;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.events.ListenerScope;
import tc.oc.pgm.events.ParticipantBlockTransformEvent;
import tc.oc.pgm.tracker.Trackers;
import tc.oc.pgm.tracker.info.BlockInfo;
import tc.oc.pgm.util.block.RayBlockIntersection;
import tc.oc.pgm.util.bukkit.Events;
import tc.oc.pgm.util.event.PlayerPunchBlockEvent;
import tc.oc.pgm.util.event.PlayerTrampleBlockEvent;
import tc.oc.pgm.util.event.entity.EntityDespawnInVoidEvent;
import tc.oc.pgm.util.material.BlockMaterialData;
import tc.oc.pgm.util.material.MaterialData;

@ListenerScope(MatchScope.RUNNING)
public class BlockDropsMatchModule implements MatchModule, Listener {
  private static final double BASE_FALL_SPEED = 3d;

  private final BlockDropsRuleSet ruleSet;

  // Tracks FallingBlocks created by explosions that have been randomly chosen
  // to not form a block when they land. We need to track them from the time
  // they are created because we don't want this to affect FallingBlocks created
  // in the normal vanilla way.
  //
  // This WILL leak a few entities now and then, because there are ways they can
  // die that do not fire an event e.g. the tick age limit, but this should be
  // rare and they will only leak until the end of the match.
  private final Set<FallingBlock> fallingBlocksThatWillNotLand = new HashSet<>();
  private final Match match;

  public BlockDropsMatchModule(Match match, BlockDropsRuleSet ruleSet) {
    this.match = match;
    this.ruleSet = ruleSet;
  }

  public BlockDropsRuleSet getRuleSet() {
    return ruleSet;
  }

  private static boolean causesDrops(final BlockTransformEvent event) {
    if (event.getOldState().getType() == Material.AIR) return false;
    final Event cause = event.getCause();
    if (cause instanceof BlockBreakEvent breakEvent)
      return event.getBlock().equals(breakEvent.getBlock());

    // Explosion-hit TNT is primed, not dropped, on both modern and legacy
    if (!MISC_UTILS.isDestructiveExplosion(cause) || event.changedFrom(Material.TNT)) return false;

    // As with breaking, doors drop from the half that the explosion hit
    return !DOORS.matches(event.getOldState().getType())
        || Events.explodedBlocks(cause).contains(event.getBlock());
  }

  @EventHandler(priority = EventPriority.LOW)
  public void initializeDrops(BlockTransformEvent event) {
    if (!causesDrops(event)) {
      return;
    }

    BlockDrops drops = this.ruleSet.getDrops(
        event, event.getOldState(), ParticipantBlockTransformEvent.getPlayerState(event));
    if (drops != null) {
      event.setDrops(drops);
    }
  }

  private void dropObjects(
      BlockDrops drops,
      @Nullable MatchPlayer player,
      Location location,
      double yield,
      boolean explosion) {
    giveKit(drops, player);
    if (explosion) {
      match
          .getExecutor(MatchScope.RUNNING)
          .execute(() -> dropItems(drops, player, location, yield));
    } else {
      dropItems(drops, player, location, yield);
      dropExperience(drops, location);
    }
  }

  private void giveKit(BlockDrops drops, MatchPlayer player) {
    if (player != null && player.isParticipating() && player.canInteract() && drops.kit != null) {
      player.applyKit(drops.kit, false);
    }
  }

  private void dropItems(BlockDrops drops, MatchPlayer player, Location location, double yield) {
    if (player == null || player.getGameMode() != GameMode.CREATIVE) {
      Random random = match.getRandom();
      for (Map.Entry<ItemStack, Double> entry : drops.items.entrySet()) {
        if (random.nextFloat() < yield * entry.getValue()) {
          Location dropLocation = location.clone();
          dropLocation.setX(dropLocation.getBlockX() + random.nextDouble() * 0.5 + 0.25);
          dropLocation.setY(dropLocation.getBlockY() + random.nextDouble() * 0.5 + 0.25);
          dropLocation.setZ(dropLocation.getBlockZ() + random.nextDouble() * 0.5 + 0.25);
          dropLocation.getWorld().dropItem(dropLocation, entry.getKey());
        }
      }
    }
  }

  private void dropExperience(BlockDrops drops, Location location) {
    if (drops.experience != 0) {
      ExperienceOrb expOrb =
          (ExperienceOrb) location.getWorld().spawnEntity(location, EntityType.EXPERIENCE_ORB);
      if (expOrb != null) {
        expOrb.setExperience(drops.experience);
      }
    }
  }

  private void replaceBlock(BlockDrops drops, Block block, MatchPlayer player) {
    if (drops.replacement != null) {
      EntityChangeBlockEvent event =
          MISC_UTILS.createEntityChangeBlockEvent(player.getBukkit(), block, drops.replacement);
      match.callEvent(event);

      if (!event.isCancelled()) {
        BlockState state = block.getState();
        drops.replacement.applyTo(state);
        state.update(true, true);
      }
    }
  }

  /**
   * This is not an event handler. It is called explicitly by BlockTransformListener after all event
   * handlers have been called.
   */
  @SuppressWarnings("deprecation")
  public void doBlockDrops(final BlockTransformEvent event) {
    if (!causesDrops(event)) {
      return;
    }

    final BlockDrops drops = event.getDrops();
    if (drops != null) {
      event.setCancelled(true);
      final BlockState oldState = event.getOldState();
      final BlockState newState = event.getNewState();
      final Block block = event.getOldState().getBlock();

      MaterialData.block(newState).applyTo(block, true);

      float yield = 1f;
      Location center = null;
      MatchPlayer player = ParticipantBlockTransformEvent.getParticipant(event);

      final Event cause = event.getCause();
      if (cause instanceof EntityExplodeEvent explosion) {
        center = explosion.getLocation();
        yield = explosion.getYield();
      } else if (cause instanceof BlockExplodeEvent explosion) {
        // getBlock is the explosion center rounded down on both platforms, and getLocation is that
        // block's corner, so re-centering it is as close as we can get to the centre itself.
        center = explosion.getBlock().getLocation().add(0.5, 0.5, 0.5);
        yield = explosion.getYield();
      }

      if (center != null
          && drops.fallChance != null
          && match.getRandom().nextFloat() < drops.fallChance) {

        FallingBlock fallingBlock =
            MaterialData.block(oldState).spawnFallingBlock(block.getLocation());
        fallingBlock.setDropItem(false);

        // Nothing else will attribute this one: the spawn fires no event the trackers can see, and
        // BlockTracker has already cleared the source block by the time drops run, so the breaker
        // this event carries is the only owner still available.
        if (player != null) {
          Trackers.trackEntity(fallingBlock, new BlockInfo(oldState, player.getParticipantState()));
        }

        if (drops.landChance != null && match.getRandom().nextFloat() >= drops.landChance) {
          this.fallingBlocksThatWillNotLand.add(fallingBlock);
        }

        Vector v = fallingBlock.getLocation().subtract(center).toVector();
        double distance = v.length();
        v.normalize().multiply(BASE_FALL_SPEED * drops.fallSpeed / Math.max(1d, distance));

        // A very simple deflection model. Check for a solid
        // neighbor block and "bounce" the velocity off of it.
        Block west = block.getRelative(BlockFace.WEST);
        Block east = block.getRelative(BlockFace.EAST);
        Block down = block.getRelative(BlockFace.DOWN);
        Block up = block.getRelative(BlockFace.UP);
        Block north = block.getRelative(BlockFace.NORTH);
        Block south = block.getRelative(BlockFace.SOUTH);

        if ((v.getX() < 0 && west != null && west.getType().isSolid())
            || v.getX() > 0 && east != null && east.getType().isSolid()) {
          v.setX(-v.getX());
        }

        if ((v.getY() < 0 && down != null && down.getType().isSolid())
            || v.getY() > 0 && up != null && up.getType().isSolid()) {
          v.setY(-v.getY());
        }

        if ((v.getZ() < 0 && north != null && north.getType().isSolid())
            || v.getZ() > 0 && south != null && south.getType().isSolid()) {
          v.setZ(-v.getZ());
        }

        fallingBlock.setVelocity(v);
      }

      dropObjects(drops, player, newState.getLocation(), yield, center != null);
    }
  }

  @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
  public void onFallingBlockLand(BlockTransformEvent event) {
    if (event.getCause() instanceof EntityChangeBlockEvent entityChangeBlockEvent) {
      Entity entity = entityChangeBlockEvent.getEntity();
      if (entity instanceof FallingBlock && this.fallingBlocksThatWillNotLand.remove(entity)) {
        event.setCancelled(true);
      }
    }
  }

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onBlockFallInVoid(EntityDespawnInVoidEvent event) {
    if (event.getEntity() instanceof FallingBlock fb) {
      this.fallingBlocksThatWillNotLand.remove(fb);
    }
  }

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onBlockPunch(PlayerPunchBlockEvent event) {
    final MatchPlayer player = match.getPlayer(event.getPlayer());
    if (player == null || !player.canInteract()) return;

    RayBlockIntersection hit = event.getRay();

    BlockDrops drops =
        getRuleSet().getDrops(event, hit.getBlock().getState(), player.getParticipantState());
    if (drops == null) return;

    BlockMaterialData oldMaterial = MaterialData.block(hit.getBlock());
    replaceBlock(drops, hit.getBlock(), player);
    Location location = hit.getPosition().toLocation(hit.getBlock().getWorld());

    EFFECTS.blockBreak(location, oldMaterial);
    dropObjects(drops, player, location, 1d, false);
  }

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onBlockTrample(PlayerTrampleBlockEvent event) {
    final MatchPlayer player = match.getPlayer(event.getPlayer());
    if (player == null || !player.canInteract()) return;

    BlockDrops drops =
        getRuleSet().getDrops(event, event.getBlock().getState(), player.getParticipantState());
    if (drops == null) return;

    replaceBlock(drops, event.getBlock(), player);

    Location location = player.getLocation();
    dropObjects(drops, player, location, 1d, false);
  }
}
