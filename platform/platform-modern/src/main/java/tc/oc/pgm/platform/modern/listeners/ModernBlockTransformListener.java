package tc.oc.pgm.platform.modern.listeners;

import java.util.List;
import org.bukkit.Material;
import org.bukkit.block.BlockState;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.Waterlogged;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.block.BlockFertilizeEvent;
import org.bukkit.event.block.CauldronLevelChangeEvent;
import org.bukkit.event.block.FluidLevelChangeEvent;
import org.bukkit.event.block.MoistureChangeEvent;
import org.bukkit.event.block.SpongeAbsorbEvent;
import org.bukkit.event.player.PlayerBucketEmptyEvent;
import org.bukkit.event.player.PlayerBucketFillEvent;
import org.bukkit.event.world.StructureGrowEvent;
import org.bukkit.plugin.Plugin;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import tc.oc.pgm.api.event.BlockTransformEvent;
import tc.oc.pgm.listeners.BlockTransformListener;
import tc.oc.pgm.platform.modern.material.ModernBlockData;
import tc.oc.pgm.tracker.Trackers;
import tc.oc.pgm.util.block.BlockStates;
import tc.oc.pgm.util.material.MaterialMatcher;

@NullMarked
public class ModernBlockTransformListener extends BlockTransformListener {

  public ModernBlockTransformListener(Plugin plugin) {
    super(plugin);
  }

  private static final MaterialMatcher CAULDRON =
      MaterialMatcher.of(m -> m.name().endsWith("CAULDRON"));

  @Override
  protected void finishCauseEvent(Event causeEvent, List<BlockTransformEvent> wrapperEvents) {
    super.finishCauseEvent(causeEvent, wrapperEvents);

    if (causeEvent instanceof BlockExplodeEvent blockExplodeEvent) {
      BlockState explodedBlockState = blockExplodeEvent.getExplodedBlockState();
      for (BlockTransformEvent wrapper : wrapperEvents) {
        if (wrapper.isCancelled() && wrapper.getOldState() == explodedBlockState) {
          explodedBlockState.update(true, false);
          break;
        }
      }
    }

    if (causeEvent instanceof SpongeAbsorbEvent sponge)
      removeCancelled(sponge.getBlocks(), wrapperEvents, BlockState::getBlock);

    if (causeEvent instanceof BlockFertilizeEvent fertilize)
      removeCancelled(fertilize.getBlocks(), wrapperEvents, BlockState::getBlock);
  }

  @Override
  @EventWrapper
  public void onPlayerBucketEmpty(final PlayerBucketEmptyEvent event) {
    // Cauldrons are handled by onCauldronLevelChange
    if (CAULDRON.matches(event.getBlock().getType())) return;

    Material contents =
        switch (event.getBucket()) {
          case LAVA_BUCKET -> Material.LAVA;
          case POWDER_SNOW_BUCKET -> Material.POWDER_SNOW;
          default -> Material.WATER;
        };

    BlockState oldBlock = event.getBlock().getState();
    BlockState waterloggedBlock =
        contents == Material.WATER ? waterloggedState(oldBlock, true) : null;
    BlockState newBlock = waterloggedBlock != null
        ? waterloggedBlock
        : BlockStates.cloneWithMaterial(event.getBlock(), contents);
    callEvent(event, oldBlock, newBlock, event.getPlayer());
  }

  private @Nullable BlockState waterloggedState(BlockState state, boolean waterlogged) {
    BlockData data = state.getBlockData();
    if (!(data instanceof Waterlogged oldData)) {
      return null;
    }
    if (oldData.isWaterlogged() == waterlogged) {
      return null;
    }

    BlockState newState = state.getBlock().getState();
    Waterlogged newData = (Waterlogged) oldData.clone();
    newData.setWaterlogged(waterlogged);
    newState.setBlockData(newData);
    return newState;
  }

  @Override
  @EventWrapper
  public void onStructureGrow(final StructureGrowEvent event) {
    // Bonemeal is handled by onBlockFertilize
    if (!event.isFromBonemeal()) {
      super.onStructureGrow(event);
    }
  }

  @Override
  @EventWrapper
  public void onPlayerBucketFill(final PlayerBucketFillEvent event) {
    if (CAULDRON.matches(event.getBlock().getType())) return;

    BlockState state = event.getBlock().getState();
    BlockState newState = waterloggedState(state, false);
    if (newState != null) {
      callEvent(event, state, newState, event.getPlayer());
      return;
    }

    // Don't count milking cows as a block transform
    boolean bucketFillSource =
        switch (state.getType()) {
          case WATER, LAVA, POWDER_SNOW -> true;
          default -> false;
        };
    if (!bucketFillSource) return;

    callEvent(event, state, BlockStates.toAir(state), event.getPlayer());
  }

  @Override
  @EventWrapper
  public void onBlockExplode(final BlockExplodeEvent event) {
    BlockState oldState = event.getExplodedBlockState();
    BlockTransformEvent transform =
        new BlockTransformEvent(event, oldState, BlockStates.toAir(oldState));
    transform.setPropagate(false);
    callEvent(transform);

    super.onBlockExplode(event);
  }

  @EventWrapper
  public void onFluidLevelChange(final FluidLevelChangeEvent event) {
    BlockState newState = event.getBlock().getState();
    new ModernBlockData(event.getNewData()).applyTo(newState);
    callEvent(new BlockTransformEvent(event, event.getBlock().getState(), newState));
  }

  @EventWrapper
  public void onCauldronLevelChange(final CauldronLevelChangeEvent event) {
    if (event.getEntity() instanceof Player player) {
      callEvent(event, event.getBlock().getState(), event.getNewState(), player);
    } else {
      var entity = event.getEntity();
      callEvent(
          event,
          event.getBlock().getState(),
          event.getNewState(),
          entity == null ? null : Trackers.getOwner(entity));
    }
  }

  @EventWrapper
  public void onSpongeAbsorb(final SpongeAbsorbEvent event) {
    for (BlockState newState : event.getBlocks()) {
      BlockTransformEvent transform =
          new BlockTransformEvent(event, newState.getBlock().getState(), newState);
      transform.setPropagate(false);
      callEvent(transform);
    }
  }

  @EventWrapper
  public void onMoistureChange(final MoistureChangeEvent event) {
    callEvent(new BlockTransformEvent(event, event.getBlock().getState(), event.getNewState()));
  }

  @EventWrapper
  public void onBlockFertilize(final BlockFertilizeEvent event) {
    for (BlockState newState : event.getBlocks()) {
      callEvent(event, newState.getBlock().getState(), newState, event.getPlayer())
          .setPropagate(false);
    }
  }
}
