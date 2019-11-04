package tc.oc.pgm.events;

import static com.google.common.base.Preconditions.checkNotNull;

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
import org.bukkit.event.entity.ExplosionPrimeByEntityEvent;
import org.bukkit.event.player.PlayerBucketEmptyEvent;
import org.bukkit.event.player.PlayerBucketFillEvent;
import org.bukkit.material.MaterialData;
import tc.oc.block.BlockStates;
import tc.oc.material.matcher.SingleMaterialMatcher;
import tc.oc.pgm.api.event.GeneralizingEvent;
import tc.oc.pgm.blockdrops.BlockDrops;
import tc.oc.util.reflect.ReflectionUtils;

/** Event called when a block transforms from one state to another. */
public class BlockTransformEvent extends GeneralizingEvent {

  protected final Block block;
  protected final BlockState oldState;
  protected final BlockState newState;
  protected BlockDrops drops;

  protected BlockTransformEvent(
      Event cause, Block block, BlockState oldState, BlockState newState) {
    super(checkNotNull(cause));
    this.block = checkNotNull(block);
    this.oldState = checkNotNull(oldState);
    this.newState = checkNotNull(newState);
  }

  public BlockTransformEvent(Event cause, BlockState oldState, BlockState newState) {
    this(cause, oldState.getBlock(), oldState, newState);
  }

  public BlockTransformEvent(Event cause, Block block, MaterialData newMaterial) {
    this(cause, block, block.getState(), BlockStates.cloneWithMaterial(block, newMaterial));
  }

  public BlockTransformEvent(Event cause, Block block, Material newMaterial) {
    this(cause, block, block.getState(), BlockStates.cloneWithMaterial(block, newMaterial));
  }

  public World getWorld() {
    return this.oldState.getWorld();
  }

  public Block getBlock() {
    return block;
  }

  public BlockState getOldState() {
    return this.oldState;
  }

  public BlockState getNewState() {
    if (this.drops == null || this.drops.replacement == null) {
      return this.newState;
    } else {
      BlockState state = this.newState.getBlock().getState();
      state.setType(this.drops.replacement.getItemType());
      state.setData(this.drops.replacement);
      return state;
    }
  }

  public boolean changedFrom(Material material) {
    return this.oldState.getType() == material && this.newState.getType() != material;
  }

  public boolean changedFrom(MaterialData material) {
    return this.oldState.getData().equals(material) && !this.newState.getData().equals(material);
  }

  public boolean changedFrom(SingleMaterialMatcher material) {
    return material.matches(this.oldState.getData()) && !material.matches(this.newState.getData());
  }

  public boolean changedTo(Material material) {
    return this.oldState.getType() != material && this.newState.getType() == material;
  }

  public boolean changedTo(MaterialData material) {
    return !this.oldState.getData().equals(material) && this.newState.getData().equals(material);
  }

  public boolean changedTo(SingleMaterialMatcher material) {
    return !material.matches(this.oldState.getData()) && material.matches(this.newState.getData());
  }

  public BlockDrops getDrops() {
    return drops;
  }

  public void setDrops(BlockDrops drops) {
    this.drops = drops;
  }

  /**
   * Return true if this is a "place" i.e. there is a non-air block here after the transform. Note
   * that a place can also be a break, if the new block replaced an existing one.
   */
  public boolean isPlace() {
    return this.newState.getType() != Material.AIR;
  }

  /**
   * Return true if this is a "break" i.e. there was a non-air block here before the transform. Note
   * that a break can also be a place, if the existing block was replaced by a new one.
   */
  public boolean isBreak() {
    return this.oldState.getType() != Material.AIR;
  }

  /**
   * Return true if the block transformation was performed "by hand".
   *
   * <p>Handled: - place - mine - bucket fill/empty - flint & steel fire/tnt
   *
   * <p>Not handled: - bonemeal - probably lots of other things
   */
  public boolean isManual() {
    final Event event = getCause();

    if (ReflectionUtils.instanceOfAny(
        event,
        BlockPlaceEvent.class,
        BlockBreakEvent.class,
        PlayerBucketEmptyEvent.class,
        PlayerBucketFillEvent.class)) return true;

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

  @Override
  public String toString() {
    return this.getClass().getSimpleName()
        + "{pos="
        + this.getOldState().getLocation().toVector()
        + " oldState="
        + this.getOldState().getData()
        + " newState="
        + this.getNewState().getData()
        + " drops="
        + this.getDrops()
        + " cancelled="
        + this.isCancelled()
        + " cause="
        + (this.getCause() == null ? "null" : this.getCause().getEventName())
        + "}";
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
