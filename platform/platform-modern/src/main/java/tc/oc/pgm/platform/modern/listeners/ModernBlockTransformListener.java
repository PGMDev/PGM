package tc.oc.pgm.platform.modern.listeners;

import static tc.oc.pgm.util.material.MaterialUtils.MATERIAL_UTILS;

import io.papermc.paper.event.block.VaultChangeStateEvent;
import java.util.List;
import java.util.function.Consumer;
import org.bukkit.Material;
import org.bukkit.Tag;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.Levelled;
import org.bukkit.block.data.Lightable;
import org.bukkit.block.data.Waterlogged;
import org.bukkit.block.data.type.TurtleEgg;
import org.bukkit.block.data.type.Vault;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.Event;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBurnEvent;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.block.BlockFertilizeEvent;
import org.bukkit.event.block.BlockFromToEvent;
import org.bukkit.event.block.BlockIgniteEvent;
import org.bukkit.event.block.CauldronLevelChangeEvent;
import org.bukkit.event.block.FluidLevelChangeEvent;
import org.bukkit.event.block.MoistureChangeEvent;
import org.bukkit.event.block.SpongeAbsorbEvent;
import org.bukkit.event.block.TNTPrimeEvent;
import org.bukkit.event.entity.EntityChangeBlockEvent;
import org.bukkit.event.entity.EntityInteractEvent;
import org.bukkit.event.player.PlayerBucketEmptyEvent;
import org.bukkit.event.player.PlayerBucketFillEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.world.StructureGrowEvent;
import org.bukkit.plugin.Plugin;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import tc.oc.pgm.api.event.BlockTransformEvent;
import tc.oc.pgm.api.player.ParticipantState;
import tc.oc.pgm.listeners.BlockTransformListener;
import tc.oc.pgm.platform.modern.material.ModernBlockData;
import tc.oc.pgm.tracker.Trackers;
import tc.oc.pgm.util.block.BlockStates;
import tc.oc.pgm.util.event.entity.ExplosionPrimeEvent;
import tc.oc.pgm.util.material.MaterialMatcher;
import tc.oc.pgm.util.material.Materials;

@NullMarked
public class ModernBlockTransformListener extends BlockTransformListener {

  public ModernBlockTransformListener(Plugin plugin) {
    super(plugin);
  }

  private static final MaterialMatcher CAULDRON =
      MaterialMatcher.of(m -> m.name().endsWith("CAULDRON"));

  @Override
  protected void finishCauseEvent(Event causeEvent, List<BlockTransformEvent> wrapperEvents) {
    if (causeEvent instanceof BlockExplodeEvent blockExplodeEvent) {
      BlockState explodedBlockState = blockExplodeEvent.getExplodedBlockState();
      for (BlockTransformEvent wrapper : wrapperEvents) {
        if (wrapper.isCancelled() && wrapper.getOldState() == explodedBlockState) {
          explodedBlockState.update(true, false);
          break;
        }
      }
    }

    super.finishCauseEvent(causeEvent, wrapperEvents);

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

    Material contents = MATERIAL_UTILS.getBucketContents(event.getBucket());
    if (contents == null) return;

    Block block = event.getBlock();
    BlockState newBlock = bucketResult(block, contents);
    if (newBlock == null) return;

    callEvent(event, block.getState(), newBlock, event.getPlayer());
  }

  private @Nullable BlockState waterloggedState(BlockState state, boolean waterlogged) {
    if (!(state.getBlockData() instanceof Waterlogged data) || data.isWaterlogged() == waterlogged)
      return null;

    return withData(state.getBlock(), data, d -> d.setWaterlogged(waterlogged));
  }

  private static <T extends BlockData> BlockState withData(
      Block block, T data, Consumer<T> mutate) {
    mutate.accept(data);
    BlockState state = block.getState();
    state.setBlockData(data);
    return state;
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
  public void onBlockFromTo(final BlockFromToEvent event) {
    if (isWaterSource(event.getBlock())) {
      BlockState oldState = event.getToBlock().getState();
      BlockState waterlogged = waterloggedState(oldState, true);
      if (waterlogged != null) {
        callEvent(event, oldState, waterlogged, Trackers.getOwner(event.getBlock()));
        return;
      }
    }

    super.onBlockFromTo(event);
  }

  @Override
  protected BlockState getFlowSourceState(Block block) {
    return isWaterSource(block) && !Materials.isWater(block.getType())
        ? BlockStates.cloneWithMaterial(block, Material.WATER)
        : super.getFlowSourceState(block);
  }

  @Override
  protected BlockState getFlowResult(BlockFromToEvent event, BlockState source) {
    BlockState newState = event.getToBlock().getState();

    // When lava flows into water, it creates stone or cobblestone
    if (Materials.isWater(newState.getType()) && Materials.isLava(source.getType())) {
      newState.setType(event.getFace() == BlockFace.DOWN ? Material.STONE : Material.COBBLESTONE);
      return newState;
    }

    newState.setType(source.getType());
    if (newState.getBlockData() instanceof Levelled level
        && source.getBlockData() instanceof Levelled sourceLevel) {
      level.setLevel(getFlowLevel(event, sourceLevel.getLevel()));
      newState.setBlockData(level);
    }
    return newState;
  }

  @Override
  @EventWrapper
  public void onEntityChangeBlock(final EntityChangeBlockEvent event) {
    if (event.getEntity() instanceof Projectile
        && event.getBlock().getType() == Material.TNT
        && event.getTo() == Material.AIR) return;
    super.onEntityChangeBlock(event);
  }

  private boolean isWaterSource(Block block) {
    if (Materials.isWater(block.getType())) return true;
    return block.getBlockData() instanceof Waterlogged waterlogged && waterlogged.isWaterlogged();
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
    if (!isBucketPickupSource(event.getBlock())) return;

    callEvent(event, state, BlockStates.toAir(state), event.getPlayer());
  }

  @Override
  protected boolean isBucketPickupSource(final Block block) {
    return super.isBucketPickupSource(block) || block.getType() == Material.POWDER_SNOW;
  }

  @Override
  @EventWrapper
  public void onPrimeTNT(final ExplosionPrimeEvent event) {
    if (TntListener.isBridgedPrimeEvent(event)) return;
    super.onPrimeTNT(event);
  }

  @EventWrapper
  public void onTNTPrime(final TNTPrimeEvent event) {
    // FIRE is handled via onBlockBurn
    if (event.getCause() == TNTPrimeEvent.PrimeCause.FIRE
        || event.getCause() == TNTPrimeEvent.PrimeCause.BLOCK_BREAK
        || event.getCause() == TNTPrimeEvent.PrimeCause.EXPLOSION) return;

    Block block = event.getBlock();
    if (event.getPrimingEntity() != null) {
      callEvent(
          event,
          block.getState(),
          BlockStates.toAir(block),
          getPrimer(event),
          true,
          event.getPrimingEntity() instanceof Player);
      return;
    }

    // Nothing responsible to deny (e.g. redstone), and the server primes regardless
    // TODO: with the next map proto bump, let filters deny these primes (propagating), as maps
    // before it rely on redstone igniting TNT under a global block-break filter
    BlockTransformEvent transform =
        createEvent(event, block.getState(), BlockStates.toAir(block), getPrimer(event));
    transform.markForced();
    callEvent(transform);
  }

  private @Nullable ParticipantState getPrimer(final TNTPrimeEvent event) {
    if (event.getPrimingEntity() != null) return Trackers.getOwner(event.getPrimingEntity());
    if (event.getPrimingBlock() != null) return Trackers.getOwner(event.getPrimingBlock());
    return null;
  }

  @Override
  protected BlockState getFallResult(final Block block) {
    if (block.getBlockData() instanceof Waterlogged waterlogged && waterlogged.isWaterlogged()) {
      return BlockStates.cloneWithMaterial(block, Material.WATER);
    }

    return super.getFallResult(block);
  }

  @Override
  protected @Nullable ParticipantState getIgniter(final BlockBurnEvent event) {
    Block igniting = event.getIgnitingBlock();
    if (igniting != null) {
      ParticipantState owner = Trackers.getOwner(igniting);
      if (owner != null) return owner;
    }
    return super.getIgniter(event);
  }

  @Override
  protected BlockState getIgniteResult(final BlockIgniteEvent event) {
    Block block = event.getBlock();
    if (block.getBlockData() instanceof Lightable lightable && !lightable.isLit()) {
      return withData(block, lightable, lit -> lit.setLit(true));
    }

    return BlockStates.cloneWithMaterial(block, fireOn(block.getRelative(BlockFace.DOWN)));
  }

  private Material fireOn(Block below) {
    return Tag.SOUL_FIRE_BASE_BLOCKS.isTagged(below.getType()) ? Material.SOUL_FIRE : Material.FIRE;
  }

  @Override
  @EventWrapper
  public void onBlockExplode(final BlockExplodeEvent event) {
    ParticipantState owner = getExplosionOwner(event);

    // The block that exploded is not in the block list, so it is reported separately.
    BlockState oldState = event.getExplodedBlockState();
    if (!oldState.getType().isAir()
        && !event.blockList().contains(oldState.getBlock())
        && !oldState.getBlockData().equals(oldState.getBlock().getBlockData())) {
      callEvent(event, oldState, BlockStates.toAir(oldState), owner, false);
    }

    handleExplosion(event, event.blockList(), owner);
  }

  @Override
  protected @Nullable ParticipantState getExplosionOwner(final BlockExplodeEvent event) {
    return Trackers.getOwner(event.getExplodedBlockState().getBlock());
  }

  @Override
  protected @Nullable BlockState bucketResult(final Block target, Material contents) {
    if (evaporates(target, contents)) return null;

    if (contents == Material.WATER || contents == Material.AIR) {
      BlockState state = target.getState();
      if (contents == Material.WATER
          && state.getBlockData() instanceof Waterlogged waterlogged
          && waterlogged.isWaterlogged()) return null;

      BlockState waterlogged = waterloggedState(state, contents == Material.WATER);
      if (waterlogged != null) return waterlogged;
    }

    return super.bucketResult(target, contents);
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
      callEvent(event, newState.getBlock().getState(), newState, stateOf(event.getPlayer()), false);
    }
  }

  @EventWrapper
  public void onVaultChangeState(final VaultChangeStateEvent event) {
    if (!(event.getBlock().getBlockData() instanceof Vault vault)) return;
    BlockState newState =
        withData(event.getBlock(), vault, data -> data.setVaultState(event.getNewState()));

    // Only a key insertion has a responsible player. For proximity activation, Paper reports
    // whichever nearby player it finds first.
    Player player = event.getNewState() == Vault.State.UNLOCKING ? event.getPlayer() : null;
    callEvent(event, event.getBlock().getState(), newState, player);
  }

  /**
   * Turtle eggs are decremented without any event of their own, so the resulting state is predicted
   * from the interaction that causes it.
   */
  @EventWrapper
  public void onPlayerInteract(final PlayerInteractEvent event) {
    if (event.getAction() != Action.PHYSICAL) return;

    Block block = event.getClickedBlock();
    if (block == null) return;

    BlockState newState = trampledTurtleEgg(block);
    if (newState != null) callEvent(event, block.getState(), newState, event.getPlayer());
  }

  @EventWrapper
  public void onEntityInteract(final EntityInteractEvent event) {
    BlockState newState = trampledTurtleEgg(event.getBlock());
    if (newState == null) return;

    callEvent(event, event.getBlock().getState(), newState, Trackers.getOwner(event.getEntity()));
  }

  /**
   * The state a turtle egg takes on when trampled. The interact event that precedes this is only
   * fired once vanilla has decided an egg breaks, so this always applies.
   */
  private @Nullable BlockState trampledTurtleEgg(Block block) {
    if (!(block.getBlockData() instanceof TurtleEgg egg)) return null;
    if (egg.getEggs() <= 1) return BlockStates.toAir(block);

    return withData(block, egg, fewer -> fewer.setEggs(fewer.getEggs() - 1));
  }
}
