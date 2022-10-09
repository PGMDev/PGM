package tc.oc.pgm.fallingblocks;

import static tc.oc.pgm.util.block.BlockVectors.blockAt;
import static tc.oc.pgm.util.block.BlockVectors.encodePos;
import static tc.oc.pgm.util.block.BlockVectors.neighborPos;

import gnu.trove.iterator.TLongObjectIterator;
import gnu.trove.map.TLongObjectMap;
import gnu.trove.map.hash.TLongObjectHashMap;
import gnu.trove.set.TLongSet;
import gnu.trove.set.hash.TLongHashSet;
import java.util.List;
import java.util.logging.Level;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.entity.FallingBlock;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.jetbrains.annotations.Nullable;
import tc.oc.pgm.api.event.BlockTransformEvent;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.match.MatchModule;
import tc.oc.pgm.api.match.MatchScope;
import tc.oc.pgm.api.match.Tickable;
import tc.oc.pgm.api.player.ParticipantState;
import tc.oc.pgm.api.time.Tick;
import tc.oc.pgm.events.ListenerScope;
import tc.oc.pgm.events.ParticipantBlockTransformEvent;
import tc.oc.pgm.util.collection.LongDeque;
import tc.oc.pgm.util.event.block.BlockFallEvent;
import tc.oc.pgm.util.material.Materials;

@ListenerScope(MatchScope.RUNNING)
public class FallingBlocksMatchModule implements MatchModule, Listener, Tickable {
  private static final BlockFace[] NEIGHBORS = {
    BlockFace.WEST, BlockFace.EAST, BlockFace.DOWN, BlockFace.UP, BlockFace.NORTH, BlockFace.SOUTH
  };

  // Maximum total blocks to search through over a single tick
  private static final int MAX_SEARCH_VISITS_PER_TICK = 4096;

  private static class MaxSearchVisitsExceeded extends Exception {}

  private int visitsThisTick, visitsWorstTick;

  private final List<FallingBlocksRule> rules;
  private final TLongObjectMap<TLongObjectMap<ParticipantState>> blockDisturbersByTick =
      new TLongObjectHashMap<>();
  private final Match match;

  public FallingBlocksMatchModule(Match match, List<FallingBlocksRule> rules) {
    this.match = match;
    this.rules = rules;
  }

  private @Nullable FallingBlocksRule ruleWithShortestDelay(BlockState block) {
    FallingBlocksRule shortest = null;
    for (FallingBlocksRule rule : this.rules) {
      if (rule.canFall(block) && (shortest == null || shortest.delay > rule.delay)) {
        shortest = rule;
      }
    }
    return shortest;
  }

  @Override
  public void tick(Match match, Tick tick) {
    fallCheck();
  }

  @Override
  public void disable() {
    match.getLogger().info("Longest search for this match: " + this.visitsWorstTick);
  }

  private void logError(MaxSearchVisitsExceeded ex) {
    match
        .getLogger()
        .log(
            Level.SEVERE,
            "Exceeded max search visits (" + MAX_SEARCH_VISITS_PER_TICK + ") for this tick",
            ex);
  }

  /**
   * Test if the given block is either self-supporting (doesn't match any falling rules) or is
   * adjacent to a supported block. The supported and unsupported arguments may contain the results
   * of previous completed searches. The blocks visited by this search will be added to one or the
   * other set depending on the final result.
   *
   * @param pos position of the block to test
   * @param supported set of blocks already known to be supported
   * @param unsupported set of blocks already known to be unsupported
   * @return true iff the given block is definitely supported
   */
  private boolean isSupported(long pos, TLongSet supported, TLongSet unsupported)
      throws MaxSearchVisitsExceeded {
    World world = match.getWorld();

    LongDeque queue = new LongDeque();
    TLongSet visited = new TLongHashSet();
    queue.add(pos);
    visited.add(pos);

    while (!queue.isEmpty()) {
      pos = queue.remove();

      if (supported.contains(pos)) {
        // If we find a block already known to be supported, it supports all blocks visited in the
        // search.
        supported.addAll(visited);
        return true;
      }

      if (++this.visitsThisTick > MAX_SEARCH_VISITS_PER_TICK) {
        throw new MaxSearchVisitsExceeded();
      }

      if (unsupported.contains(pos)) {
        // Don't continue the search through blocks known to be unsupported
        continue;
      }

      Block block = blockAt(world, pos);
      if (block == null) continue;

      boolean selfSupporting = true;
      for (FallingBlocksRule rule : this.rules) {
        if (rule.canFall(block)) {
          // If a rule matches, this block is not self-supporting,
          // and its status depends on the final result of the search.
          selfSupporting = false;

          // Continue the search through any neighbors that are capable of
          // supporting this block, and have not yet been visited.
          for (BlockFace face : NEIGHBORS) {
            long neighborPos = neighborPos(pos, face);
            if (!visited.contains(neighborPos)) {
              Block neighbor = blockAt(world, neighborPos);
              if (rule.canSupport(neighbor, face)) {
                queue.add(neighborPos);
                visited.add(neighborPos);
              }
            }
          }
        }
      }

      if (selfSupporting) {
        // If no rules match this block, then it is self-supporting,
        // and it can support all the other visited blocks.
        supported.addAll(visited);
        return true;
      }
    }

    // If the entire block network has been searched without finding a
    // supported block, then we know the entire network is unsupported.
    unsupported.addAll(visited);
    return false;
  }

  /**
   * Return the number of unsupported blocks connected to any blocks neighboring the given location,
   * which is assumed to contain an air block. The search may bail out early when the count is
   * greater or equal to the given limit, though this cannot be guaranteed.
   */
  private int countUnsupportedNeighbors(long pos, int limit) {
    TLongSet supported = new TLongHashSet();
    TLongSet unsupported = new TLongHashSet();

    try {
      for (BlockFace face : NEIGHBORS) {
        if (!this.isSupported(neighborPos(pos, face), supported, unsupported)) {
          if (unsupported.size() >= limit) break;
        }
      }
    } catch (MaxSearchVisitsExceeded ex) {
      this.logError(ex);
    }

    return unsupported.size();
  }

  /**
   * Return the number of unsupported blocks connected to any blocks neighboring the given location.
   * An air block is placed there temporarily if it is not already air. The search may bail out
   * early when the count is >= the given limit, though this cannot be guaranteed.
   */
  public int countUnsupportedNeighbors(Block block, int limit) {
    BlockState state = null;
    if (block.getType() != Material.AIR) {
      state = block.getState();
      block.setTypeIdAndData(0, (byte) 0, false);
    }

    int count = countUnsupportedNeighbors(encodePos(block), limit);

    if (state != null) {
      block.setTypeIdAndData(state.getTypeId(), state.getRawData(), false);
    }

    return count;
  }

  /** Make any unsupported blocks fall that are disturbed for the current tick */
  private void fallCheck() {
    this.visitsWorstTick = Math.max(this.visitsWorstTick, this.visitsThisTick);
    this.visitsThisTick = 0;

    World world = match.getWorld();
    TLongObjectMap<ParticipantState> blockDisturbers =
        this.blockDisturbersByTick.remove(match.getTick().tick);
    if (blockDisturbers == null) return;

    TLongSet supported = new TLongHashSet();
    TLongSet unsupported = new TLongHashSet();
    TLongObjectMap<ParticipantState> fallsByBreaker = new TLongObjectHashMap<>();

    try {
      while (!blockDisturbers.isEmpty()) {
        long pos = blockDisturbers.keySet().iterator().next();
        ParticipantState breaker = blockDisturbers.remove(pos);

        // Search down for the first block that can actually fall
        for (; ; ) {
          long below = neighborPos(pos, BlockFace.DOWN);
          if (!Materials.isSolid(blockAt(world, below).getType())) break;
          blockDisturbers.remove(pos); // Remove all the blocks we find along the way
          pos = below;
        }

        // Check if the block needs to fall, if it isn't already falling
        if (!fallsByBreaker.containsKey(pos) && !this.isSupported(pos, supported, unsupported)) {
          fallsByBreaker.put(pos, breaker);
        }
      }
    } catch (MaxSearchVisitsExceeded ex) {
      this.logError(ex);
    }

    for (TLongObjectIterator<ParticipantState> iter = fallsByBreaker.iterator(); iter.hasNext(); ) {
      iter.advance();
      this.fall(iter.key(), iter.value());
    }
  }

  @SuppressWarnings("deprecation")
  private void fall(long pos, @Nullable ParticipantState breaker) {
    // Block must be removed BEFORE spawning the FallingBlock, or it will not appear on the client
    // https://bugs.mojang.com/browse/MC-72248
    Block block = blockAt(match.getWorld(), pos);
    BlockState oldState = block.getState();
    block.setType(Material.AIR, false);
    FallingBlock fallingBlock =
        block
            .getWorld()
            .spawnFallingBlock(block.getLocation(), oldState.getType(), oldState.getRawData());

    BlockFallEvent event = new BlockFallEvent(block, fallingBlock);
    match.callEvent(
        breaker == null
            ? new BlockTransformEvent(event, block, Material.AIR)
            : new ParticipantBlockTransformEvent(event, block, Material.AIR, breaker));

    if (event.isCancelled()) {
      fallingBlock.remove();
      oldState.update(true, false); // Restore the old block if the fall is cancelled
    } else {
      block.setType(
          Material.AIR,
          true); // This is already air, but physics have not been applied yet, so do that now
    }
  }

  private void disturb(long pos, BlockState blockState, @Nullable ParticipantState disturber) {
    FallingBlocksRule rule = this.ruleWithShortestDelay(blockState);
    if (rule != null) {
      long tick = match.getTick().tick + rule.delay;
      TLongObjectMap<ParticipantState> blockDisturbers = this.blockDisturbersByTick.get(tick);

      if (blockDisturbers == null) {
        blockDisturbers = new TLongObjectHashMap<>();
        this.blockDisturbersByTick.put(tick, blockDisturbers);
      }

      Block block = blockState.getBlock();
      if (!blockDisturbers.containsKey(pos)) {
        blockDisturbers.put(pos, disturber);
      }
    }
  }

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onBlockChange(BlockTransformEvent event) {
    BlockState newState = event.getNewState();
    Block block = newState.getBlock();
    long pos = encodePos(block);

    // Only breaks are credited. Making a bridge fall by updating a block
    // does not credit you with breaking the bridge.
    ParticipantState breaker =
        event.isBreak() ? ParticipantBlockTransformEvent.getPlayerState(event) : null;

    if (!(event.getCause() instanceof BlockFallEvent)) {
      this.disturb(pos, newState, breaker);
    }

    for (BlockFace face : NEIGHBORS) {
      this.disturb(neighborPos(pos, face), block.getRelative(face).getState(), breaker);
    }
  }
}
