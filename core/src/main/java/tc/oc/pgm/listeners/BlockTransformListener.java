package tc.oc.pgm.listeners;

import static tc.oc.pgm.util.bukkit.MiscUtils.MISC_UTILS;
import static tc.oc.pgm.util.material.MaterialUtils.MATERIAL_UTILS;
import static tc.oc.pgm.util.material.Materials.ANY_FIRE;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ListMultimap;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.reflect.InvocationTargetException;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.logging.Logger;
import java.util.stream.Stream;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.entity.Player;
import org.bukkit.entity.TNTPrimed;
import org.bukkit.event.Event;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockBurnEvent;
import org.bukkit.event.block.BlockDispenseEvent;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.block.BlockFadeEvent;
import org.bukkit.event.block.BlockFormEvent;
import org.bukkit.event.block.BlockFromToEvent;
import org.bukkit.event.block.BlockGrowEvent;
import org.bukkit.event.block.BlockIgniteEvent;
import org.bukkit.event.block.BlockMultiPlaceEvent;
import org.bukkit.event.block.BlockPistonEvent;
import org.bukkit.event.block.BlockPistonExtendEvent;
import org.bukkit.event.block.BlockPistonRetractEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.block.BlockSpreadEvent;
import org.bukkit.event.block.LeavesDecayEvent;
import org.bukkit.event.entity.EntityChangeBlockEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.player.PlayerBucketEmptyEvent;
import org.bukkit.event.player.PlayerBucketFillEvent;
import org.bukkit.event.world.StructureGrowEvent;
import org.bukkit.plugin.EventExecutor;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginManager;
import org.bukkit.util.Vector;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import tc.oc.pgm.api.PGM;
import tc.oc.pgm.api.event.BlockTransformEvent;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.api.player.MatchPlayerState;
import tc.oc.pgm.api.player.ParticipantState;
import tc.oc.pgm.blockdrops.BlockDropsMatchModule;
import tc.oc.pgm.events.ParticipantBlockTransformEvent;
import tc.oc.pgm.events.PlayerBlockTransformEvent;
import tc.oc.pgm.tracker.Trackers;
import tc.oc.pgm.tracker.trackers.BlockTracker;
import tc.oc.pgm.util.ClassLogger;
import tc.oc.pgm.util.block.BlockFaces;
import tc.oc.pgm.util.block.BlockStates;
import tc.oc.pgm.util.bukkit.Events;
import tc.oc.pgm.util.event.block.BlockFallEvent;
import tc.oc.pgm.util.event.entity.ExplosionPrimeByEntityEvent;
import tc.oc.pgm.util.event.entity.ExplosionPrimeEvent;
import tc.oc.pgm.util.material.Materials;

@NullMarked
public class BlockTransformListener implements Listener {
  @Retention(RetentionPolicy.RUNTIME)
  public @interface EventWrapper {}

  protected final Logger logger;
  protected final Plugin plugin;
  protected final PluginManager pm;
  protected final ListMultimap<Event, BlockTransformEvent> currentEvents =
      ArrayListMultimap.create();

  public BlockTransformListener(Plugin plugin) {
    this.logger = ClassLogger.get(plugin.getLogger(), getClass());
    this.plugin = plugin;
    this.pm = plugin.getServer().getPluginManager();
  }

  public void registerEvents() {
    // Find all the @EventWrapper methods in this class and register them at EVERY priority level.
    Stream.of(getClass().getMethods())
        .filter(method -> method.getAnnotation(EventWrapper.class) != null)
        .forEach(method -> {
          final Class<? extends Event> eventClass =
              method.getParameterTypes()[0].asSubclass(Event.class);

          for (final EventPriority priority : EventPriority.values()) {
            EventExecutor executor = (listener, event) -> {
              // At the first priority level, call the event handler method. If it decides to
              // generate a BlockTransformEvent, it will be stored in currentEvents.
              if (!Events.isCancelled(event)
                  && priority == EventPriority.LOWEST
                  && eventClass.isInstance(event)) {
                try {
                  method.invoke(listener, event);
                } catch (InvocationTargetException ex) {
                  throw MISC_UTILS.createEventException(ex.getCause(), event);
                } catch (Throwable t) {
                  throw MISC_UTILS.createEventException(t, event);
                }
              }

              // Check for cached events and dispatch them at the current priority level
              // only.
              // The BTE needs to be dispatched even after it's cancelled, because we DO
              // have
              // listeners that depend on receiving cancelled events e.g. WoolMatchModule.
              for (BlockTransformEvent bte : currentEvents.get(event)) {
                Events.callEvent(bte, priority);
              }

              // After dispatching the last priority level, clean up the cached events and
              // do
              // post-event stuff.
              // This needs to happen even if the event is cancelled.
              if (priority == EventPriority.MONITOR) {
                finishCauseEvent(event);
              }
            };

            pm.registerEvent(eventClass, this, priority, executor, plugin, false);
          }
        });
  }

  private void finishCauseEvent(Event causeEvent) {
    List<BlockTransformEvent> wrapperEvents = currentEvents.removeAll(causeEvent);

    finishCauseEvent(causeEvent, wrapperEvents);
    for (BlockTransformEvent bte : wrapperEvents) processCancelMessage(bte);
  }

  // A few of the event handlers need to do some post-processing after the wrapper events return.
  protected void finishCauseEvent(Event causeEvent, List<BlockTransformEvent> wrapperEvents) {
    for (BlockTransformEvent bte : wrapperEvents) processBlockDrops(bte);

    if (causeEvent instanceof EntityExplodeEvent entityExplodeEvent)
      removeCancelled(entityExplodeEvent.blockList(), wrapperEvents, Function.identity());

    if (causeEvent instanceof BlockExplodeEvent blockExplodeEvent)
      removeCancelled(blockExplodeEvent.blockList(), wrapperEvents, Function.identity());

    if (causeEvent instanceof StructureGrowEvent structureGrowEvent)
      removeCancelled(structureGrowEvent.getBlocks(), wrapperEvents, BlockState::getBlock);

    if (causeEvent instanceof BlockPistonEvent blockPistonEvent)
      finishPistonMove(blockPistonEvent, wrapperEvents);
  }

  private void handleDoor(BlockTransformEvent event, BlockFace relative) {
    BlockState other = event.getBlock().getRelative(relative).getState();
    if (other.getType() != event.getOldState().getType()
        || MATERIAL_UTILS.getDoorOtherHalf(other) != relative.getOppositeFace()
        // Blocks an explosion reports itself, so a door half caught in it is not reported twice
        || Events.explodedBlocks(event.getCause()).contains(other.getBlock())) return;

    BlockTransformEvent toCall = createEvent(
        event.getCause(),
        other,
        BlockStates.toAir(other),
        PlayerBlockTransformEvent.getPlayerState(event));
    toCall.setPropagate(event.isPropagate());
    if (event.isManual()) toCall.markManual();
    callEvent(toCall, true);
  }

  protected void callEvent(final BlockTransformEvent event, boolean checked) {
    if (!checked) {
      BlockState old = event.getOldState();
      if (Materials.DOORS.matches(old.getType())
          && !Materials.DOORS.matches(event.getNewState().getType())) {
        BlockFace relative = MATERIAL_UTILS.getDoorOtherHalf(old);
        if (relative != null) handleDoor(event, relative);
      }
    }
    logger.finest(() -> "Generated event " + event);
    currentEvents.put(event.getCause(), event);
  }

  protected void callEvent(final BlockTransformEvent event) {
    callEvent(event, false);
  }

  protected void callEvent(
      Event cause, BlockState oldState, BlockState newState, @Nullable Player player) {
    callEvent(cause, oldState, newState, stateOf(player));
  }

  protected void callEvent(
      Event cause, BlockState oldState, BlockState newState, @Nullable MatchPlayerState player) {
    callEvent(cause, oldState, newState, player, true);
  }

  protected void callEvent(
      Event cause,
      BlockState oldState,
      BlockState newState,
      @Nullable MatchPlayerState player,
      boolean propagate) {
    callEvent(cause, oldState, newState, player, propagate, false);
  }

  protected void callEvent(
      Event cause,
      BlockState oldState,
      BlockState newState,
      @Nullable MatchPlayerState player,
      boolean propagate,
      boolean manual) {
    BlockTransformEvent event = createEvent(cause, oldState, newState, player);
    event.setPropagate(propagate);
    if (manual) event.markManual();
    callEvent(event);
  }

  protected @Nullable MatchPlayerState stateOf(@Nullable Player player) {
    MatchPlayer matchPlayer = PGM.get().getMatchManager().getPlayer(player);
    return matchPlayer == null ? null : matchPlayer.getState();
  }

  protected static BlockTransformEvent createEvent(
      Event cause, BlockState oldState, BlockState newState, @Nullable MatchPlayerState player) {
    if (player == null) return new BlockTransformEvent(cause, oldState, newState);
    if (player instanceof ParticipantState participant)
      return new ParticipantBlockTransformEvent(cause, oldState, newState, participant);
    return new PlayerBlockTransformEvent(cause, oldState, newState, player);
  }

  // ------------------------
  // ---- Placing blocks ----
  // ------------------------

  @EventWrapper
  public void onBlockPlace(final BlockPlaceEvent event) {
    if (event instanceof BlockMultiPlaceEvent blockMultiPlaceEvent) {
      for (BlockState oldState : blockMultiPlaceEvent.getReplacedBlockStates()) {
        callEvent(event, oldState, oldState.getBlock().getState(), event.getPlayer());
      }
    } else {
      callEvent(
          event, event.getBlockReplacedState(), event.getBlock().getState(), event.getPlayer());
    }
  }

  @EventWrapper
  public void onPlayerBucketEmpty(final PlayerBucketEmptyEvent event) {
    Block block = event.getBlockClicked().getRelative(event.getBlockFace());
    Material contents = MATERIAL_UTILS.getBucketContents(event.getBucket());
    if (contents == null) return;

    BlockState newBlock = bucketResult(block, contents);
    if (newBlock == null) return;

    this.callEvent(event, block.getState(), newBlock, event.getPlayer());
  }

  @EventWrapper
  public void onStructureGrow(final StructureGrowEvent event) {
    for (BlockState block : event.getBlocks()) {
      BlockTransformEvent transform =
          new BlockTransformEvent(event, block.getLocation().getBlock().getState(), block);
      transform.setPropagate(false);
      callEvent(transform);
    }
  }

  @EventWrapper
  public void onBlockGrow(final BlockGrowEvent event) {
    this.callEvent(
        new BlockTransformEvent(event, event.getBlock().getState(), event.getNewState()));
  }

  @EventWrapper
  public void onBlockForm(final BlockFormEvent event) {
    this.callEvent(
        new BlockTransformEvent(event, event.getBlock().getState(), event.getNewState()));
  }

  @EventWrapper
  public void onBlockSpread(final BlockSpreadEvent event) {
    // This fires for: fire, grass, mycelium, mushrooms, and vines
    // Fire is already handled by BlockIgniteEvent
    if (!ANY_FIRE.matches(event.getNewState().getType())) {
      this.callEvent(
          new BlockTransformEvent(event, event.getBlock().getState(), event.getNewState()));
    }
  }

  @EventWrapper
  public void onBlockFromTo(final BlockFromToEvent event) {
    BlockState source = getFlowSourceState(event.getBlock());
    if (event.getToBlock().getType() != source.getType()) {
      BlockState oldState = event.getToBlock().getState();
      BlockState newState = getFlowResult(event, source);

      // Check for lava ownership
      this.callEvent(event, oldState, newState, Trackers.getOwner(event.getBlock()));
    }
  }

  protected BlockState getFlowSourceState(Block block) {
    return block.getState();
  }

  /** The state the destination block of a flow will be in, using legacy data values. */
  @SuppressWarnings("deprecation")
  protected BlockState getFlowResult(BlockFromToEvent event, BlockState source) {
    BlockState newState = event.getToBlock().getState();
    newState.setType(source.getType());
    newState.setRawData(source.getRawData());

    // TODO: getType is deprecated getMaterial and setMaterial are SportPaper only
    // When lava flows into water, it creates stone or cobblestone
    if (Materials.isWater(event.getToBlock().getType()) && Materials.isLava(newState.getType())) {
      newState.setType(event.getFace() == BlockFace.DOWN ? Material.STONE : Material.COBBLESTONE);
      newState.setRawData((byte) 0);
    }

    // For some reason, the newState has the data value of the old source.
    // This corrects for that manually.
    if (Materials.isWater(newState.getType()) || Materials.isLava(newState.getType())) {
      newState.setRawData((byte) getFlowLevel(event, newState.getRawData()));
    }

    return newState;
  }

  protected static int getFlowLevel(BlockFromToEvent event, int sourceLevel) {
    // A level of 8 (or higher) represents liquid flowing down
    if (event.getFace() == BlockFace.DOWN) return 8;
    // Levels 0-7 represent liquid on the ground, and increase by 1 as they spread
    if (sourceLevel < 7) return sourceLevel + 1;
    // Otherwise, the previous block must have been flowing down, so it spreads to a level of 1
    return 1;
  }

  @EventWrapper
  public void onBlockIgnite(final BlockIgniteEvent event) {
    // Handled by onBlockPlace
    if (isIgnitePlacement(event)) return;

    BlockState oldState = event.getBlock().getState();
    BlockState newState = getIgniteResult(event);
    ParticipantState igniter = null;

    if (event.getIgnitingEntity() != null) {
      // The player themselves, or any of several types of owned entity starting or spreading a
      // fire.
      igniter = Trackers.getOwner(event.getIgnitingEntity());
    } else if (event.getIgnitingBlock() != null) {
      // Fire, lava, or flint & steel in a dispenser
      igniter = Trackers.getOwner(event.getIgnitingBlock());
    }

    callEvent(
        event, oldState, newState, igniter, true, event.getIgnitingEntity() instanceof Player);
  }

  /** Whether the platform also generates a {@link BlockPlaceEvent} for this ignition */
  protected boolean isIgnitePlacement(final BlockIgniteEvent event) {
    return (event.getCause() == BlockIgniteEvent.IgniteCause.FLINT_AND_STEEL
            && event.getIgnitingEntity() != null)
        || (event.getCause() == BlockIgniteEvent.IgniteCause.FIREBALL
            && event.getIgnitingEntity() instanceof Player);
  }

  protected BlockState getIgniteResult(final BlockIgniteEvent event) {
    return BlockStates.cloneWithMaterial(event.getBlock(), Material.FIRE);
  }

  // -------------------------
  // ---- Breaking blocks ----
  // -------------------------

  @EventWrapper
  public void onBlockBreak(final BlockBreakEvent event) {
    BlockState state = event.getBlock().getState();
    this.callEvent(event, state, BlockStates.toAir(state), event.getPlayer());
  }

  @EventWrapper
  public void onPlayerBucketFill(final PlayerBucketFillEvent event) {
    BlockState state = event.getBlockClicked().getRelative(event.getBlockFace()).getState();
    this.callEvent(event, state, BlockStates.toAir(state), event.getPlayer());
  }

  @EventWrapper
  public void onPrimeTNT(final ExplosionPrimeEvent event) {
    if (event.getEntity() instanceof TNTPrimed) {
      Block block = event.getEntity().getLocation().getBlock();
      if (isBeingPlaced(block)) return;
      if (block.getType() == Material.TNT) {
        if (event instanceof ExplosionPrimeByEntityEvent explosionPrimeByEntityEvent) {
          ParticipantState player = Trackers.getOwner(explosionPrimeByEntityEvent.getPrimer());
          callEvent(event, block.getState(), BlockStates.toAir(block), player);
        } else {
          // Nothing responsible to deny, and the platform primes regardless
          // TODO: with the next map proto bump, let filters deny these primes (propagating), as
          // maps before it rely on redstone igniting TNT under a global block-break filter
          BlockTransformEvent transform =
              createEvent(event, block.getState(), BlockStates.toAir(block), null);
          transform.markForced();
          callEvent(transform);
        }
      }
    }
  }

  private boolean isBeingPlaced(Block block) {
    for (Event cause : currentEvents.keySet()) {
      if (cause instanceof BlockPlaceEvent place && place.getBlock().equals(block)) return true;
    }
    return false;
  }

  @EventWrapper
  public void onEntityExplode(final EntityExplodeEvent event) {
    handleExplosion(event, event.blockList(), Trackers.getOwner(event.getEntity()));
  }

  @EventWrapper
  public void onBlockExplode(final BlockExplodeEvent event) {
    handleExplosion(event, event.blockList(), getExplosionOwner(event));
  }

  protected void handleExplosion(
      Event event, List<Block> blockList, @Nullable ParticipantState owner) {
    boolean destructive = MISC_UTILS.isDestructiveExplosion(event);
    for (Block block : blockList) {
      // Don't cancel the explosion when individual blocks are cancelled
      callEvent(
          event,
          block.getState(),
          destructive ? BlockStates.toAir(block) : block.getState(),
          owner,
          false);
    }
  }

  protected @Nullable ParticipantState getExplosionOwner(final BlockExplodeEvent event) {
    return Trackers.getOwner(event.getBlock());
  }

  protected <T> void removeCancelled(
      List<T> elements, Collection<BlockTransformEvent> wrapperEvents, Function<T, Block> toBlock) {
    Set<Block> cancelled = null;
    for (BlockTransformEvent wrapper : wrapperEvents) {
      if (wrapper.isCancelled()) {
        if (cancelled == null) cancelled = new HashSet<>();
        cancelled.add(wrapper.getOldState().getBlock());
      }
    }
    if (cancelled == null) return;

    Set<Block> blocks = cancelled;
    elements.removeIf(element -> blocks.contains(toBlock.apply(element)));
  }

  @EventWrapper
  public void onBlockBurn(final BlockBurnEvent event) {
    BlockState oldState = event.getBlock().getState();
    this.callEvent(event, oldState, BlockStates.toAir(oldState), getIgniter(event));
  }

  protected @Nullable ParticipantState getIgniter(final BlockBurnEvent event) {
    BlockTracker tracker = Trackers.getBlockTracker(event.getBlock().getWorld());
    if (tracker == null) return null;

    for (BlockFace face : BlockFaces.NEIGHBORS) {
      Block neighbor = event.getBlock().getRelative(face);
      if (ANY_FIRE.matches(neighbor.getType())) {
        ParticipantState igniter = tracker.getOwner(neighbor);
        if (igniter != null) return igniter;
      }
    }

    return null;
  }

  @EventWrapper
  public void onBlockFade(final BlockFadeEvent event) {
    this.callEvent(
        new BlockTransformEvent(event, event.getBlock().getState(), event.getNewState()));
  }

  @EventWrapper
  public void onLeavesDecay(final LeavesDecayEvent event) {
    this.callEvent(new BlockTransformEvent(
        event, event.getBlock().getState(), BlockStates.toAir(event.getBlock())));
  }

  // -----------------------
  // ---- Moving blocks ----
  // -----------------------

  private void onPistonMove(
      BlockPistonEvent event,
      List<Block> blocks,
      Map<Block, BlockState> newStates,
      Map<Block, BlockState> oldStates,
      Predicate<BlockState> nonPropagating) {
    // The block list in a piston event includes only the pushed blocks, not the empty spaces they
    // are
    // pushed into. We need to build our own map of the post-event block states.

    // Add the pushed blocks at their destination
    for (Block block : blocks) {
      if (MATERIAL_UTILS.isBrokenByPiston(block)) continue;
      Block dest = block.getRelative(event.getDirection());
      newStates.put(dest, BlockStates.cloneWithMaterial(dest, block.getState()));
    }

    // Add air blocks where a block is leaving, and no other block is replacing it
    for (Block block : blocks) {
      if (!newStates.containsKey(block)) {
        newStates.put(block, BlockStates.toAir(block.getState()));
      }
    }

    // Fire events for all changing blocks.
    for (Map.Entry<Block, BlockState> entry : newStates.entrySet()) {
      Block block = entry.getKey();
      BlockState oldState = oldStates.getOrDefault(block, block.getState());
      BlockTransformEvent bte = new BlockTransformEvent(event, oldState, entry.getValue());
      bte.setPropagate(!nonPropagating.test(entry.getValue()));
      this.callEvent(bte);
    }
  }

  private void finishPistonMove(
      BlockPistonEvent causeEvent, Collection<BlockTransformEvent> wrapperEvents) {
    // If ANY of the pushed block events are cancelled, the piston jams and the entire causing event
    // is cancelled. Non-propagating transforms are reported but cannot jam the piston.
    for (BlockTransformEvent bte : wrapperEvents) {
      if (bte.isCancelled() && bte.isPropagate()) {
        causeEvent.setCancelled(true);
        break;
      }
    }
  }

  @EventWrapper
  public void onBlockPistonExtend(final BlockPistonExtendEvent event) {
    Map<Block, BlockState> newStates = new HashMap<>();

    Block head = event.getBlock().getRelative(event.getDirection());
    newStates.put(head, pistonHeadState(head, event.getDirection(), event.isSticky()));

    this.onPistonMove(event, event.getBlocks(), newStates, Map.of(), _ -> false);
  }

  /** The state of an extended piston arm at {@code head}, facing {@code facing}. */
  private BlockState pistonHeadState(Block head, BlockFace facing, boolean sticky) {
    BlockState state = head.getState();
    MATERIAL_UTILS.pistonHead(facing, sticky).applyTo(state);
    return state;
  }

  @EventWrapper
  public void onBlockPistonRetract(final BlockPistonRetractEvent event) {
    if (MISC_UTILS.isDuplicateRetract(event)) return;

    BlockFace facing = getPistonFacing(event);
    Block head = event.getBlock().getRelative(facing);

    BlockState armLeaving = BlockStates.toAir(head);
    Map<Block, BlockState> newStates = new HashMap<>();
    newStates.put(head, armLeaving);

    // Modern clears the arm before firing this event, while SportPaper restores it first.
    // Air here therefore means the arm was just removed, so its old state is synthesised rather
    // than read; otherwise a sticky pull would report air as the old state on modern only.
    Map<Block, BlockState> oldStates = head.getType() == Material.AIR
        ? Map.of(head, pistonHeadState(head, facing, event.isSticky()))
        : Map.of();

    // A retracting arm is reported, but cannot jam the piston by being cancelled. If a pulled
    // block took the arm's place, onPistonMove overwrites armLeaving in newStates before firing
    // anything, so the transform is a real block move that may be cancelled as usual.
    this.onPistonMove(event, event.getBlocks(), newStates, oldStates, state -> state == armLeaving);
  }

  /**
   * getDirection is not consistent across the code paths that fire a piston event: the ones that
   * move blocks report the direction those blocks travel, while the ones with an empty block list
   * report the piston's facing. Read the facing off the piston itself where possible, since by this
   * point it may have already been replaced with a moving piston, which carries no facing on the
   * legacy platform.
   *
   * <p>The two inconsistencies cancel out: the only retract event that loses the facing is the one
   * that reports a travel direction, so inverting getDirection is correct wherever it is reached.
   */
  private BlockFace getPistonFacing(final BlockPistonRetractEvent event) {
    BlockFace facing = MATERIAL_UTILS.getFacingOrNull(event.getBlock());
    return facing != null ? facing : event.getDirection().getOppositeFace();
  }

  // -----------------------------
  // ---- Transforming blocks ----
  // -----------------------------
  @EventWrapper
  public void onEntityChangeBlock(final EntityChangeBlockEvent event) {
    // Handled by onBlockFall
    if (MISC_UTILS.getFallingBlock(event) != null) return;

    callEvent(
        event,
        event.getBlock().getState(),
        BlockStates.cloneWithMaterial(event.getBlock(), MATERIAL_UTILS.getTo(event)),
        Trackers.getOwner(event.getEntity()));
  }

  @EventWrapper
  public void onDispenserDispense(final BlockDispenseEvent event) {
    Material contents = MATERIAL_UTILS.getBucketContents(event.getItem().getType());
    if (contents == null) return;

    Block target = dispenseTarget(event);
    if (target == null) return;

    BlockState newState = bucketResult(target, contents);
    if (newState == null) return;

    callEvent(event, target.getState(), newState, Trackers.getOwner(event.getBlock()));
  }

  // Yes, the location the dispenser is facing is stored in "velocity" for some ungodly reason
  private @Nullable Block dispenseTarget(final BlockDispenseEvent event) {
    BlockFace facing = MATERIAL_UTILS.getFacingOrNull(event.getBlock());
    if (facing == null) return null;

    Block target = event.getBlock().getRelative(facing);
    Vector position = event.getVelocity();
    return position.getX() == target.getX()
            && position.getY() == target.getY()
            && position.getZ() == target.getZ()
        ? target
        : null;
  }

  /** The state {@code target} takes on when a bucket's {@code contents} are poured onto it. */
  protected @Nullable BlockState bucketResult(final Block target, Material contents) {
    if (evaporates(target, contents)) return null;
    if (contents == Material.AIR && !isBucketPickupSource(target)) return null;
    return BlockStates.cloneWithMaterial(target, contents);
  }

  /** Whether pouring {@code contents} onto {@code target} places nothing. */
  protected boolean evaporates(final Block target, Material contents) {
    return Materials.isWater(contents) && MISC_UTILS.doesWaterEvaporate(target);
  }

  /** Whether an empty bucket can pick this block up, and so leave air behind. */
  protected boolean isBucketPickupSource(final Block block) {
    return block.isLiquid();
  }

  @EventWrapper
  public void onBlockFall(final BlockFallEvent event) {
    Block block = event.getBlock();
    this.callEvent(new BlockTransformEvent(event, block.getState(), getFallResult(block)));
  }

  protected BlockState getFallResult(final Block block) {
    return BlockStates.toAir(block);
  }

  // --------------------------
  // ---- Event Processing ----
  // --------------------------

  public void processCancelMessage(final BlockTransformEvent event) {
    if (event instanceof PlayerBlockTransformEvent bte
        && event.isCancelled()
        && event.getCancellationReason() != null
        && event.isManual()) {

      bte.getPlayerState().sendWarning(event.getCancellationReason());
    }
  }

  @SuppressWarnings("deprecation")
  public void processBlockDrops(BlockTransformEvent event) {
    // If the event has been altered with custom block drops/replacement,
    // call on the BlockDropsMatchModule to handle this. We do this here
    // because doBlockDrops will cancel the event, and we don't want any
    // other listeners to think the event is cancelled when it isn't.
    if (!event.isCancelled() && event.getDrops() != null) {
      Match match = PGM.get().getMatchManager().getMatch(event.getWorld());
      if (match != null) {
        BlockDropsMatchModule bdmm = match.getModule(BlockDropsMatchModule.class);
        if (bdmm != null) {
          bdmm.doBlockDrops(event);
        }
      }
    }
  }
}
