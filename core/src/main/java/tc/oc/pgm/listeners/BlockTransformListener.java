package tc.oc.pgm.listeners;

import static tc.oc.pgm.util.bukkit.MiscUtils.MISC_UTILS;
import static tc.oc.pgm.util.material.MaterialUtils.MATERIAL_UTILS;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ListMultimap;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.reflect.InvocationTargetException;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import java.util.stream.Stream;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.entity.Player;
import org.bukkit.entity.TNTPrimed;
import org.bukkit.event.Event;
import org.bukkit.event.EventException;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockBurnEvent;
import org.bukkit.event.block.BlockDispenseEvent;
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
import org.bukkit.event.entity.EntityChangeBlockEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.player.PlayerBucketEmptyEvent;
import org.bukkit.event.player.PlayerBucketFillEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.world.StructureGrowEvent;
import org.bukkit.material.Door;
import org.bukkit.plugin.EventExecutor;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginManager;
import org.jetbrains.annotations.Nullable;
import tc.oc.pgm.api.PGM;
import tc.oc.pgm.api.event.BlockTransformEvent;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.api.player.MatchPlayerState;
import tc.oc.pgm.api.player.ParticipantState;
import tc.oc.pgm.blockdrops.BlockDropsMatchModule;
import tc.oc.pgm.events.ParticipantBlockTransformEvent;
import tc.oc.pgm.events.PlayerBlockTransformEvent;
import tc.oc.pgm.tracker.TrackerMatchModule;
import tc.oc.pgm.tracker.Trackers;
import tc.oc.pgm.util.ClassLogger;
import tc.oc.pgm.util.block.BlockStates;
import tc.oc.pgm.util.bukkit.Events;
import tc.oc.pgm.util.event.block.BlockFallEvent;
import tc.oc.pgm.util.event.entity.ExplosionPrimeByEntityEvent;
import tc.oc.pgm.util.event.entity.ExplosionPrimeEvent;
import tc.oc.pgm.util.material.Materials;

public class BlockTransformListener implements Listener {
  private static final BlockFace[] NEIGHBORS = {
    BlockFace.WEST, BlockFace.EAST, BlockFace.DOWN, BlockFace.UP, BlockFace.NORTH, BlockFace.SOUTH
  };

  @Retention(RetentionPolicy.RUNTIME)
  @interface EventWrapper {}

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
            EventExecutor executor = new EventExecutor() {
              @Override
              public void execute(Listener listener, Event event) throws EventException {
                // REMOVED: Ignore the event if it was fron a non-Match world
                // if (event instanceof Physical
                //    && PGM.get().getMatchManager().getMatch(((Physical) event).getWorld())
                // ==
                // null)
                //  return;

                if (!Events.isCancelled(event)) {
                  // At the first priority level, call the event handler method.
                  // If it decides to generate a BlockTransformEvent, it will be stored in
                  // currentEvents.
                  if (priority == EventPriority.LOWEST) {
                    if (eventClass.isInstance(event)) {
                      try {
                        method.invoke(listener, event);
                      } catch (InvocationTargetException ex) {
                        throw MISC_UTILS.createEventException(ex.getCause(), event);
                      } catch (Throwable t) {
                        throw MISC_UTILS.createEventException(t, event);
                      }
                    }
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
              }
            };

            pm.registerEvent(eventClass, this, priority, executor, plugin, false);
          }
        });
  }

  private void finishCauseEvent(Event causeEvent) {
    List<BlockTransformEvent> wrapperEvents = currentEvents.removeAll(causeEvent);

    // A few of the event handlers need to do some post-processing after the wrapper event returns.
    if (causeEvent instanceof EntityExplodeEvent) {
      finishEntityExplode((EntityExplodeEvent) causeEvent, wrapperEvents);
    } else if (causeEvent instanceof BlockPistonEvent) {
      finishPistonMove((BlockPistonEvent) causeEvent, wrapperEvents);
    }

    for (BlockTransformEvent bte : wrapperEvents) {
      processCancelMessage(bte);
    }

    for (BlockTransformEvent bte : wrapperEvents) {
      processBlockDrops(bte);
    }
  }

  private void handleDoor(BlockTransformEvent event, Door door) {
    BlockFace relative = door.isTopHalf() ? BlockFace.DOWN : BlockFace.UP;
    BlockState oldState = event.getOldState().getBlock().getRelative(relative).getState();
    BlockState newState = event.getBlock().getRelative(relative).getState();
    BlockTransformEvent toCall;
    if (event instanceof ParticipantBlockTransformEvent bte) {
      toCall = new ParticipantBlockTransformEvent(
          event.getCause(), oldState, newState, bte.getPlayerState());
    } else if (event instanceof PlayerBlockTransformEvent bte) {
      toCall =
          new PlayerBlockTransformEvent(event.getCause(), oldState, newState, bte.getPlayerState());
    } else {
      toCall = new BlockTransformEvent(event, oldState, newState);
    }
    callEvent(toCall, true);
  }

  private void callEvent(final BlockTransformEvent event, boolean checked) {
    if (!checked) {
      org.bukkit.material.MaterialData oldData = event.getOldState().getData();
      org.bukkit.material.MaterialData newData = event.getNewState().getData();
      if (oldData instanceof Door) {
        handleDoor(event, (Door) oldData);
      }
      if (newData instanceof Door) {
        handleDoor(event, (Door) newData);
      }
    }
    logger.finest("Generated event " + event);
    currentEvents.put(event.getCause(), event);
  }

  private void callEvent(final BlockTransformEvent event) {
    callEvent(event, false);
  }

  private BlockTransformEvent callEvent(
      Event cause, BlockState oldState, BlockState newState, @Nullable Player player) {
    MatchPlayer matchPlayer = PGM.get().getMatchManager().getPlayer(player);
    return callEvent(
        cause, oldState, newState, matchPlayer == null ? null : matchPlayer.getState());
  }

  private BlockTransformEvent callEvent(
      Event cause, BlockState oldState, BlockState newState, @Nullable MatchPlayerState player) {
    BlockTransformEvent event;
    if (player == null) {
      event = new BlockTransformEvent(cause, oldState, newState);
    } else if (player instanceof ParticipantState) {
      event =
          new ParticipantBlockTransformEvent(cause, oldState, newState, (ParticipantState) player);
    } else {
      event = new PlayerBlockTransformEvent(cause, oldState, newState, player);
    }
    callEvent(event);
    return event;
  }

  // ------------------------
  // ---- Placing blocks ----
  // ------------------------

  @EventWrapper
  public void onBlockPlace(final BlockPlaceEvent event) {
    if (event instanceof BlockMultiPlaceEvent) {
      for (BlockState oldState : ((BlockMultiPlaceEvent) event).getReplacedBlockStates()) {
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
    Material contents = Materials.materialInBucket(event.getBucket());
    if (contents == null) {
      return;
    }
    BlockState newBlock = BlockStates.cloneWithMaterial(block, contents);

    this.callEvent(event, block.getState(), newBlock, event.getPlayer());
  }

  @EventWrapper
  public void onStructureGrow(final StructureGrowEvent event) {
    for (BlockState block : event.getBlocks()) {
      this.callEvent(
          new BlockTransformEvent(event, block.getLocation().getBlock().getState(), block));
    }
  }

  @EventWrapper
  public void onBlockForm(final BlockGrowEvent event) {
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
    if (event.getNewState().getType() != Material.FIRE) {
      this.callEvent(
          new BlockTransformEvent(event, event.getBlock().getState(), event.getNewState()));
    }
  }

  @SuppressWarnings("deprecation")
  @EventWrapper
  public void onBlockFromTo(BlockFromToEvent event) {
    if (event.getToBlock().getType() != event.getBlock().getType()) {
      BlockState oldState = event.getToBlock().getState();
      BlockState newState = event.getToBlock().getState();
      newState.setType(event.getBlock().getType());
      newState.setRawData(event.getBlock().getData());

      // TODO: getType is deprecated getMaterial and setMaterial are SportPaper only
      // When lava flows into water, it creates stone or cobblestone
      if (Materials.isWater(oldState.getType()) && Materials.isLava(newState.getType())) {
        newState.setType(event.getFace() == BlockFace.DOWN ? Material.STONE : Material.COBBLESTONE);
        newState.setRawData((byte) 0);
      }

      // For some reason, the newState has the data value of the old source.
      // This corrects for that manually.
      if (Materials.isWater(newState.getType()) || Materials.isLava(newState.getType())) {
        byte oldData = newState.getRawData();
        if (event.getFace() == BlockFace.DOWN) {
          // A data value of 8 (or higher) represents water flowing down
          newState.setRawData((byte) (8));
        } else if (oldData < 7) {
          // Data values 0-7 represent water on the ground, and increase by 1 as they spread
          newState.setRawData((byte) (oldData + 1));
        } else {
          // Otherwise, the previous block must have been flowing down, so it spreads to a data
          // value of 1
          newState.setRawData((byte) (1));
        }
      }

      // Check for lava ownership
      this.callEvent(event, oldState, newState, Trackers.getOwner(event.getBlock()));
    }
  }

  @EventWrapper
  public void onBlockIgnite(final BlockIgniteEvent event) {
    // Flint & steel generates a BlockPlaceEvent
    if (event.getCause() == BlockIgniteEvent.IgniteCause.FLINT_AND_STEEL) return;

    BlockState oldState = event.getBlock().getState();
    BlockState newState = BlockStates.cloneWithMaterial(event.getBlock(), Material.FIRE);
    ParticipantState igniter = null;

    if (event.getIgnitingEntity() != null) {
      // The player themselves using flint & steel, or any of
      // several types of owned entity starting or spreading a fire.
      igniter = Trackers.getOwner(event.getIgnitingEntity());
    } else if (event.getIgnitingBlock() != null) {
      // Fire, lava, or flint & steel in a dispenser
      igniter = Trackers.getOwner(event.getIgnitingBlock());
    }

    callEvent(event, oldState, newState, igniter);
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

  @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
  public void onPrimeTNT(ExplosionPrimeEvent event) {
    if (event.getEntity() instanceof TNTPrimed) {
      Block block = event.getEntity().getLocation().getBlock();
      if (block.getType() == Material.TNT) {
        ParticipantState player;
        if (event instanceof ExplosionPrimeByEntityEvent) {
          player = Trackers.getOwner(((ExplosionPrimeByEntityEvent) event).getPrimer());
        } else {
          player = null;
        }
        callEvent(event, block.getState(), BlockStates.toAir(block), player);
      }
    }
  }

  @EventWrapper
  public void onEntityExplode(final EntityExplodeEvent event) {
    ParticipantState playerState = Trackers.getOwner(event.getEntity());

    for (Block block : event.blockList()) {
      if (block.getType() != Material.TNT) {
        // Don't cancel the explosion when individual blocks are cancelled
        callEvent(event, block.getState(), BlockStates.toAir(block), playerState)
            .setPropagate(false);
      }
    }
  }

  private void finishEntityExplode(
      EntityExplodeEvent causeEvent, Collection<BlockTransformEvent> wrapperEvents) {
    // Remove blocks from the explosion if their wrapper event was cancelled
    for (BlockTransformEvent wrapper : wrapperEvents) {
      if (wrapper.isCancelled()) {
        causeEvent.blockList().remove(wrapper.getOldState().getBlock());
      }
    }
  }

  @EventWrapper
  public void onBlockBurn(final BlockBurnEvent event) {
    Match match = PGM.get().getMatchManager().getMatch(event.getBlock().getWorld());
    if (match == null) return;

    BlockState oldState = event.getBlock().getState();
    BlockState newState = BlockStates.toAir(oldState);
    MatchPlayerState igniterState = null;
    TrackerMatchModule tmm = match.needModule(TrackerMatchModule.class);

    for (BlockFace face : NEIGHBORS) {
      Block neighbor = oldState.getBlock().getRelative(face);
      if (neighbor.getType() == Material.FIRE) {
        igniterState = tmm.getOwner(neighbor);
        if (igniterState != null) break;
      }
    }

    this.callEvent(event, oldState, newState, igniterState);
  }

  @EventWrapper
  public void onBlockFade(final BlockFadeEvent event) {
    BlockState state = event.getBlock().getState();
    this.callEvent(new BlockTransformEvent(event, state, BlockStates.toAir(state)));
  }

  // -----------------------
  // ---- Moving blocks ----
  // -----------------------

  private void onPistonMove(
      BlockPistonEvent event, List<Block> blocks, Map<Block, BlockState> newStates) {
    // The block list in a piston event includes only the pushed blocks, not the empty spaces they
    // are
    // pushed into. We need to build our own map of the post-event block states.

    // Add the pushed blocks at their destination
    for (Block block : blocks) {
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
    for (BlockState newState : newStates.values()) {
      this.callEvent(new BlockTransformEvent(event, newState.getBlock().getState(), newState));
    }
  }

  private void finishPistonMove(
      BlockPistonEvent causeEvent, Collection<BlockTransformEvent> wrapperEvents) {
    // If ANY of the pushed block events are cancelled, the piston jams and the entire causing event
    // is cancelled.
    for (BlockTransformEvent bte : wrapperEvents) {
      if (bte.isCancelled()) {
        causeEvent.setCancelled(true);
        break;
      }
    }
  }

  @EventWrapper
  public void onBlockPistonExtend(final BlockPistonExtendEvent event) {
    Map<Block, BlockState> newStates = new HashMap<>();

    // Add the arm of the piston, which will extend into the adjacent block.
    BlockState state = event.getBlock().getRelative(event.getDirection()).getState();
    MATERIAL_UTILS
        .fromLegacyBlock(Materials.PISTON_HEAD, getPistonDirectionByte(event.getDirection()))
        .applyTo(state);
    newStates.put(event.getBlock(), state);

    this.onPistonMove(event, event.getBlocks(), newStates);
  }

  private byte getPistonDirectionByte(BlockFace face) {
    return switch (face) {
      default -> 0; // down included
      case UP -> 1;
      case NORTH -> 2;
      case SOUTH -> 3;
      case WEST -> 4;
      case EAST -> 5;
    };
  }

  @EventWrapper
  public void onBlockPistonRetract(final BlockPistonRetractEvent event) {
    this.onPistonMove(event, event.getBlocks(), new HashMap<Block, BlockState>());
  }

  // -----------------------------
  // ---- Transforming blocks ----
  // -----------------------------
  @EventWrapper
  public void onEntityChangeBlock(final EntityChangeBlockEvent event) {
    callEvent(
        event,
        event.getBlock().getState(),
        BlockStates.cloneWithMaterial(event.getBlock(), MATERIAL_UTILS.getTo(event)),
        Trackers.getOwner(event.getEntity()));
  }

  @EventWrapper
  public void onBlockTrample(final PlayerInteractEvent event) {
    if (event.getAction() == Action.PHYSICAL) {
      Block block = event.getClickedBlock();
      if (block != null) {
        Material oldType = getTrampledType(block.getType());
        if (oldType != null) {
          callEvent(
              event,
              BlockStates.cloneWithMaterial(block, oldType),
              block.getState(),
              event.getPlayer());
        }
      }
    }
  }

  @EventWrapper
  public void onDispenserDispense(final BlockDispenseEvent event) {
    if (Materials.isBucket(event.getItem())) {
      // Yes, the location the dispenser is facing is stored in "velocity" for some ungodly reason
      Block targetBlock =
          event.getVelocity().toLocation(event.getBlock().getWorld()).getBlock();
      Material contents = Materials.materialInBucket(event.getItem());

      if (Materials.isLiquid(contents) || (contents == Material.AIR && targetBlock.isLiquid())) {
        callEvent(
            event,
            targetBlock.getState(),
            BlockStates.cloneWithMaterial(targetBlock, contents),
            Trackers.getOwner(event.getBlock()));
      }
    }
  }

  @EventWrapper
  public void onBlockFall(BlockFallEvent event) {
    this.callEvent(new BlockTransformEvent(
        event, event.getBlock().getState(), BlockStates.toAir(event.getBlock().getState())));
  }

  private static Material getTrampledType(Material newType) {
    if (newType == Materials.SOIL) return Material.DIRT;
    return null;
  }

  // --------------------------
  // ---- Event Processing ----
  // --------------------------

  public void processCancelMessage(final BlockTransformEvent event) {
    if (event instanceof PlayerBlockTransformEvent
        && event.isCancelled()
        && event.getCancellationReason() != null
        && event.isManual()) {

      ((PlayerBlockTransformEvent) event)
          .getPlayerState()
          .sendWarning(event.getCancellationReason());
    }
  }

  public void processBlockDrops(BlockTransformEvent event) {
    // If the event has been altered with custom block drops/replacement,
    // call on the BlockDropsMatchModule to handle this. We do this here
    // because doBlockDrops will cancel the event, and we don't want any
    // other listeners to think the event is cancelled when it isn't.
    if (event != null && !event.isCancelled() && event.getDrops() != null) {
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
