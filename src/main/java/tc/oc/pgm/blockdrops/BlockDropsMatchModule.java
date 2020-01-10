package tc.oc.pgm.blockdrops;

import java.util.HashSet;
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
import org.bukkit.event.entity.EntityChangeBlockEvent;
import org.bukkit.event.entity.EntityDespawnInVoidEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.material.MaterialData;
import org.bukkit.util.RayBlockIntersection;
import org.bukkit.util.Vector;
import tc.oc.pgm.api.event.BlockPunchEvent;
import tc.oc.pgm.api.event.BlockTrampleEvent;
import tc.oc.pgm.api.event.BlockTransformEvent;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.match.MatchModule;
import tc.oc.pgm.api.match.MatchScope;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.events.ListenerScope;
import tc.oc.pgm.events.ParticipantBlockTransformEvent;
import tc.oc.util.Pair;
import tc.oc.world.NMSHacks;

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

  public static boolean causesDrops(final Event event) {
    return event instanceof BlockBreakEvent || event instanceof EntityExplodeEvent;
  }

  @EventHandler(priority = EventPriority.LOW)
  public void initializeDrops(BlockTransformEvent event) {
    if (!causesDrops(event.getCause())) {
      return;
    }

    BlockDrops drops =
        this.ruleSet.getDrops(
            event, event.getOldState(), ParticipantBlockTransformEvent.getPlayerState(event));
    if (drops != null) {
      event.setDrops(drops);
    }
  }

  private void dropItems(BlockDrops drops, Location location, double yield) {
    Random random = match.getRandom();
    for (Pair<Double, ItemStack> entry : drops.items) {
      if (random.nextFloat() < yield * entry.first) {
        location.getWorld().dropItemNaturally(location, entry.second);
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
          new EntityChangeBlockEvent(
              player.getBukkit(),
              block,
              drops.replacement.getItemType(),
              drops.replacement.getData());
      match.callEvent(event);

      if (!event.isCancelled()) {
        BlockState state = block.getState();
        state.setType(drops.replacement.getItemType());
        state.setData(drops.replacement);
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
    if (!causesDrops(event.getCause())) {
      return;
    }

    final BlockDrops drops = event.getDrops();
    if (drops != null) {
      event.setCancelled(true);
      final BlockState oldState = event.getOldState();
      final BlockState newState = event.getNewState();
      final Block block = event.getOldState().getBlock();
      final int newTypeId = newState.getTypeId();
      final byte newData = newState.getRawData();

      block.setTypeIdAndData(newTypeId, newData, true);

      if (event.getCause() instanceof EntityExplodeEvent) {
        EntityExplodeEvent explodeEvent = (EntityExplodeEvent) event.getCause();
        final float yield = explodeEvent.getYield();

        if (drops.fallChance != null
            && oldState.getType().isBlock()
            && oldState.getType() != Material.AIR
            && match.getRandom().nextFloat() < drops.fallChance) {

          FallingBlock fallingBlock =
              match
                  .getWorld()
                  .spawnFallingBlock(
                      block.getLocation(),
                      event.getOldState().getType(),
                      event.getOldState().getRawData());
          fallingBlock.setDropItem(false);

          if (drops.landChance != null && match.getRandom().nextFloat() >= drops.landChance) {
            this.fallingBlocksThatWillNotLand.add(fallingBlock);
          }

          Vector v = fallingBlock.getLocation().subtract(explodeEvent.getLocation()).toVector();
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

        // Defer item drops so the explosion doesn't destroy them
        match
            .getScheduler(MatchScope.RUNNING)
            .runTask(
                new Runnable() {
                  @Override
                  public void run() {
                    dropItems(drops, newState.getLocation(), yield);
                  }
                });
      } else {
        MatchPlayer player = ParticipantBlockTransformEvent.getParticipant(event);
        if (player == null
            || player.getBukkit().getGameMode()
                != GameMode.CREATIVE) { // Don't drop items in creative mode
          dropItems(drops, newState.getLocation(), 1d);
          dropExperience(drops, newState.getLocation());
        }
      }
    }
  }

  @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
  public void onFallingBlockLand(BlockTransformEvent event) {
    if (event.getCause() instanceof EntityChangeBlockEvent) {
      Entity entity = ((EntityChangeBlockEvent) event.getCause()).getEntity();
      if (entity instanceof FallingBlock && this.fallingBlocksThatWillNotLand.remove(entity)) {
        event.setCancelled(true);
      }
    }
  }

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onBlockFallInVoid(EntityDespawnInVoidEvent event) {
    if (event.getEntity() instanceof FallingBlock) {
      this.fallingBlocksThatWillNotLand.remove(event.getEntity());
    }
  }

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onBlockPunch(BlockPunchEvent event) {
    final MatchPlayer player = match.getPlayer(event.getPlayer());
    if (player == null) return;

    RayBlockIntersection hit = event.getIntersection();

    BlockDrops drops =
        getRuleSet().getDrops(event, hit.getBlock().getState(), player.getParticipantState());
    if (drops == null) return;

    MaterialData oldMaterial = hit.getBlock().getState().getData();
    replaceBlock(drops, hit.getBlock(), player);

    // Play a fake punching effect if the block is punchable. Use raw particles instead of
    // playBlockBreakEffect so the position is precise rather than in the block center.
    Object packet =
        NMSHacks.blockCrackParticlesPacket(
            oldMaterial, false, hit.getPosition(), new Vector(), 0, 5);
    for (MatchPlayer viewer : match.getPlayers()) {
      if (viewer.getBukkit().getEyeLocation().toVector().distanceSquared(hit.getPosition())
          < 16 * 16) {
        NMSHacks.sendPacket(viewer.getBukkit(), packet);
      }
    }
    NMSHacks.playBlockPlaceSound(
        hit.getBlock().getWorld(), hit.getPosition(), oldMaterial.getItemType(), 1);

    Location location = hit.getPosition().toLocation(hit.getBlock().getWorld());
    dropItems(drops, location, 1d);
    dropExperience(drops, location);
  }

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onBlockTrample(BlockTrampleEvent event) {
    final MatchPlayer player = match.getPlayer(event.getPlayer());
    if (player == null) return;

    BlockDrops drops =
        getRuleSet().getDrops(event, event.getBlock().getState(), player.getParticipantState());
    if (drops == null) return;

    replaceBlock(drops, event.getBlock(), player);

    Location location = player.getBukkit().getLocation();
    dropItems(drops, location, 1d);
    dropExperience(drops, location);
  }
}
