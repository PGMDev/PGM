package tc.oc.pgm.api.event;

import static tc.oc.pgm.util.Assert.assertNotNull;
import static tc.oc.pgm.util.Assert.assertTrue;

import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockIgniteEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerBucketEmptyEvent;
import org.bukkit.event.player.PlayerBucketFillEvent;
import org.bukkit.material.MaterialData;
import tc.oc.pgm.blockdrops.BlockDrops;
import tc.oc.pgm.util.block.BlockStates;
import tc.oc.pgm.util.event.GeneralizedEvent;
import tc.oc.pgm.util.event.entity.ExplosionPrimeByEntityEvent;

/** Called when a {@link Block} transforms from one {@link BlockState} to another. */
public class BlockTransformEvent extends GeneralizedEvent {

  private final Block block;
  private final BlockState oldState;
  private final BlockState newState;

  // FIXME: Orthogonal concern from block drops module, remove later
  private BlockDrops drops;

  public BlockTransformEvent(Event cause, Block block, BlockState oldState, BlockState newState) {
    super(assertNotNull(cause));
    this.block = assertNotNull(block);
    this.oldState = assertNotNull(oldState);
    this.newState = assertNotNull(newState);
    assertTrue(block.getWorld().equals(oldState.getWorld()));
    assertTrue(block.getWorld().equals(newState.getWorld()));
  }

  public BlockTransformEvent(Event cause, BlockState oldState, BlockState newState) {
    this(cause, assertNotNull(oldState).getBlock(), oldState, newState);
  }

  public BlockTransformEvent(Event cause, Block block, MaterialData newMaterial) {
    this(
        cause,
        block,
        assertNotNull(block).getState(),
        BlockStates.cloneWithMaterial(block, newMaterial));
  }

  public BlockTransformEvent(Event cause, Block block, Material newMaterial) {
    this(
        cause,
        block,
        assertNotNull(block).getState(),
        BlockStates.cloneWithMaterial(block, newMaterial));
  }

  /**
   * Get the {@link World} that the {@link BlockTransformEvent} occurred in.
   *
   * @return The {@link World} of the event.
   */
  public final World getWorld() {
    return oldState.getWorld();
  }

  /**
   * Get the {@link Block} that was transformed.
   *
   * @return The transformed {@link Block}.
   */
  public final Block getBlock() {
    return block;
  }

  /**
   * Get the {@link BlockState} before the {@link Block} was transformed.
   *
   * @return The old {@link BlockState}.
   */
  public final BlockState getOldState() {
    return oldState;
  }

  /**
   * Get the {@link BlockState} after the {@link Block} was transformed.
   *
   * @return The current {@link BlockState}.
   */
  public final BlockState getNewState() {
    if (drops == null || drops.replacement == null) {
      return newState;
    } else {
      final BlockState state = newState.getBlock().getState();
      state.setType(drops.replacement.getItemType());
      state.setData(drops.replacement);
      return state;
    }
  }

  /**
   * Get whether the transformation changed from a specific {@link Material}.
   *
   * @param material The {@link Material} of the {@link #getOldState()}.
   * @return Whether the given {@link Material} equals the {@link #getOldState()}.
   */
  public final boolean changedFrom(Material material) {
    return oldState.getType() == material && newState.getType() != material;
  }

  /**
   * Get whether the transformation changed to a specific {@link Material}.
   *
   * @param material The {@link Material} of the {@link #getNewState()}}.
   * @return Whether the given {@link Material} equals the {@link #getNewState()}}.
   */
  public final boolean changedTo(Material material) {
    return oldState.getType() != material && newState.getType() == material;
  }

  /**
   * Get whether the transformation resulted in a "place," where the {@link #getNewState()}} is
   * non-air.
   *
   * @return Whether the {@link #getNewState()}} is non-air.
   */
  public final boolean isPlace() {
    return newState.getType() != Material.AIR;
  }

  /**
   * Get whether the transformation resulted in a "break," where the {@link #getOldState()} is
   * non-air.
   *
   * @return Whether the {@link #getOldState()} is non-air.
   */
  public final boolean isBreak() {
    return oldState.getType() != Material.AIR;
  }

  /**
   * Get whether the {@link Block} was probably transformed by a player.
   *
   * @return Whether the event is considered "manual."
   */
  public final boolean isManual() {
    final Event event = getCause();

    if (event instanceof BlockPlaceEvent
        || event instanceof BlockBreakEvent
        || event instanceof PlayerBucketEmptyEvent
        || event instanceof PlayerBucketFillEvent) return true;

    if (event instanceof BlockIgniteEvent) {
      BlockIgniteEvent igniteEvent = (BlockIgniteEvent) event;
      if (igniteEvent.getCause() == BlockIgniteEvent.IgniteCause.FLINT_AND_STEEL
          && igniteEvent.getIgnitingEntity() != null) {
        return true;
      }
    }

    if (event instanceof ExplosionPrimeByEntityEvent
        && ((ExplosionPrimeByEntityEvent) event).getPrimer() instanceof Player) {
      return true;
    }

    return false;
  }

  @Deprecated
  public final BlockDrops getDrops() {
    return drops;
  }

  @Deprecated
  public final void setDrops(BlockDrops drops) {
    this.drops = drops;
  }

  private static final HandlerList handlers = new HandlerList();

  @Override
  public HandlerList getHandlers() {
    return handlers;
  }

  public static HandlerList getHandlerList() {
    return handlers;
  }
}
