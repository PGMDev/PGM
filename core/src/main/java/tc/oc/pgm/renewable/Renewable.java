package tc.oc.pgm.renewable;

import static tc.oc.pgm.util.bukkit.Effects.EFFECTS;

import com.google.common.collect.ImmutableRangeMap;
import com.google.common.collect.Range;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.logging.Logger;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.util.BlockVector;
import tc.oc.pgm.api.event.BlockTransformEvent;
import tc.oc.pgm.api.filter.Filter;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.match.Tickable;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.api.time.Tick;
import tc.oc.pgm.filters.query.BlockQuery;
import tc.oc.pgm.snapshot.SnapshotMatchModule;
import tc.oc.pgm.util.ClassLogger;
import tc.oc.pgm.util.block.BlockFaces;
import tc.oc.pgm.util.block.BlockVectorSet;
import tc.oc.pgm.util.block.BlockVectors;
import tc.oc.pgm.util.material.BlockMaterialData;
import tc.oc.pgm.util.material.MaterialCounter;
import tc.oc.pgm.util.material.MaterialData;

public class Renewable implements Listener, Tickable {

  private static final int MAX_FAILED_ITERATIONS = 100;
  private static final int SHUFFLE_SAMPLE_ITERATIONS = 10;
  private static final int SHUFFLE_SAMPLE_RANGE = 5;

  private final RenewableDefinition definition;
  private final Match match;
  private final Logger logger;
  // Current inverse distribution of shuffleable world relative to the initial state (which is
  // unknown).
  // This is used to choose a world when renewing shuffleable blocks.
  private final MaterialCounter shuffleableMaterialDeficit = new MaterialCounter();

  // Set of blocks that are immediately renewable, dynamically updated from block events.
  // Maintaining this set avoids nearly all trial and error logic in the renewal tick.
  private final BlockVectorSet renewablePool = new BlockVectorSet();

  // Number of blocks that currently must to be renewed to keep up with the configured rate.
  private long lastTick;

  private SnapshotMatchModule snapshotMatchModule;

  // Cached queries of the renewable/shuffleable filters, invalidated every tick.
  // These are queries of the original blocks, not the current blocks.
  // This should cut down on repeated queries.
  private Map<BlockVector, Filter.QueryResponse> renewableCache = new HashMap<>();
  private Map<BlockVector, Filter.QueryResponse> shuffleableCache = new HashMap<>();

  public Renewable(RenewableDefinition definition, Match match, Logger parent) {
    this.definition = definition;
    this.match = match;
    this.logger = ClassLogger.get(parent, getClass());

    updateLastTick();
  }

  void invalidateCaches() {
    renewableCache.clear();
    shuffleableCache.clear();
  }

  SnapshotMatchModule snapshot() {
    if (snapshotMatchModule == null) {
      snapshotMatchModule = match.needModule(SnapshotMatchModule.class);
    }
    return snapshotMatchModule;
  }

  boolean isOriginalRenewable(Block block) {
    if (!definition.region.contains(block)) return false;
    BlockVector pos = BlockVectors.position(block);
    Filter.QueryResponse response = renewableCache.get(pos);
    if (response == null) {
      response = definition.renewableBlocks.query(new BlockQuery(snapshot().getOriginalBlock(pos)));
    }
    return response.isAllowed();
  }

  boolean isOriginalShuffleable(Block block) {
    if (!definition.region.contains(block)) return false;
    BlockVector pos = BlockVectors.position(block);
    Filter.QueryResponse response = shuffleableCache.get(pos);
    if (response == null) {
      response =
          definition.shuffleableBlocks.query(new BlockQuery(snapshot().getOriginalBlock(pos)));
    }
    return response.isAllowed();
  }

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onBlockChange(BlockTransformEvent event) {
    BlockState oldState = event.getOldState(), newState = event.getNewState();

    updateRenewablePool(newState);

    if (definition.growAdjacent) {
      for (BlockFace face : BlockFaces.NEIGHBORS) {
        updateRenewablePool(BlockFaces.getRelative(newState, face));
      }
    }

    if (isOriginalShuffleable(event.getBlock())) {
      if (definition.shuffleableBlocks.query(new BlockQuery(oldState)).isAllowed()) {
        shuffleableMaterialDeficit.increment(oldState, 1);
      }

      if (definition.shuffleableBlocks.query(new BlockQuery(newState)).isAllowed()) {
        shuffleableMaterialDeficit.increment(newState, -1);
      }
    }
  }

  @Override
  public void tick(Match match, Tick tick) {
    invalidateCaches();

    float interval = updateLastTick(); // should always be 1
    float count = interval * definition.renewalsPerSecond / 20f; // calculate renewals per tick
    if (definition.rateScaled) count *= renewablePool.size();

    for (; count > 0 && !renewablePool.isEmpty(); count--) {
      if (match.getRandom().nextFloat() < count) {
        for (int i = 0; i < MAX_FAILED_ITERATIONS; i++) {
          BlockVector pos = renewablePool.chooseRandom(match.getRandom());
          if (renew(pos)) break;
        }
      }
    }
  }

  long updateLastTick() {
    long delta = match.getTick().tick - lastTick;
    lastTick = match.getTick().tick;
    return delta;
  }

  void updateRenewablePool(BlockState block) {
    if (canRenew(block)) {
      renewablePool.add(BlockVectors.position(block));
    } else {
      renewablePool.remove(BlockVectors.position(block));
    }
  }

  boolean isNew(BlockState currentState) {
    // If original block does not match renewable rule, block is new
    if (!isOriginalRenewable(currentState.getBlock())) return true;

    // If original and current world are both shuffleable, block is new
    MaterialData currentMaterial = MaterialData.block(currentState);
    if (isOriginalShuffleable(currentState.getBlock())
        && definition.shuffleableBlocks.query(new BlockQuery(currentState)).isAllowed())
      return true;

    // If current world matches original, block is new
    if (currentMaterial.equals(snapshot().getOriginalMaterial(BlockVectors.position(currentState))))
      return true;

    // Otherwise, block is not new (can be renewed)
    return false;
  }

  boolean hasNewNeighbor(BlockState block) {
    for (BlockFace face : BlockFaces.NEIGHBORS) {
      if (isNew(BlockFaces.getRelative(block, face))) return true;
    }
    return false;
  }

  boolean canRenew(BlockState currentState) {
    // Must not already be new
    if (isNew(currentState)) return false;

    // Must grow from an adjacent block that is renewed
    if (definition.growAdjacent && !hasNewNeighbor(currentState)) return false;

    // Current block must be replaceable
    if (!definition.replaceableBlocks.query(new BlockQuery(currentState)).isAllowed()) return false;

    return true;
  }

  boolean isClearOfEntities(BlockVector pos) {
    if (definition.avoidPlayersRange > 0d) {
      double rangeSquared = definition.avoidPlayersRange * definition.avoidPlayersRange;
      pos = BlockVectors.center(pos);
      for (MatchPlayer player : match.getParticipants()) {
        Location location = player.getBukkit().getLocation().add(0, 1, 0);
        if (location.toVector().distanceSquared(pos) < rangeSquared) {
          return false;
        }
      }
    }
    return true;
  }

  BlockMaterialData sampleShuffledMaterial(BlockVector pos) {
    Random random = match.getRandom();
    int range = SHUFFLE_SAMPLE_RANGE;
    int diameter = range * 2 + 1;
    for (int i = 0; i < SHUFFLE_SAMPLE_ITERATIONS; i++) {
      BlockState block = snapshot()
          .getOriginalBlock(
              pos.getBlockX() + random.nextInt(diameter) - range,
              pos.getBlockY() + random.nextInt(diameter) - range,
              pos.getBlockZ() + random.nextInt(diameter) - range);
      if (definition.shuffleableBlocks.query(new BlockQuery(block)).isAllowed())
        return MaterialData.block(block);
    }
    return null;
  }

  BlockMaterialData chooseShuffledMaterial() {
    var weightsBuilder = ImmutableRangeMap.<Double, BlockMaterialData>builder();
    double sum = 0d;
    for (BlockMaterialData material : shuffleableMaterialDeficit.materials()) {
      double weight = shuffleableMaterialDeficit.get(material);
      if (weight > 0) {
        weightsBuilder.put(Range.closedOpen(sum, sum + weight), material);
        sum += weight;
      }
    }
    var weights = weightsBuilder.build();
    return weights.get(match.getRandom().nextDouble() * sum);
  }

  boolean renew(BlockVector pos) {
    BlockMaterialData material;
    if (isOriginalShuffleable(BlockVectors.blockAt(match.getWorld(), pos))) {
      // If position is shuffled, first try to find a nearby shuffleable block to swap with.
      // This helps to make shuffling less predictable when the world deficit is small or
      // out of proportion to the original distribution of world.
      material = sampleShuffledMaterial(pos);

      // If that fails, choose a random world, weighted by the current world deficits.
      if (material == null) material = chooseShuffledMaterial();
    } else {
      material = snapshot().getOriginalMaterial(pos);
    }

    if (material != null) {
      return renew(pos, material);
    }

    return false;
  }

  boolean renew(BlockVector pos, BlockMaterialData material) {
    // We need to do the entity check here rather than canRenew, because we are not
    // notified when entities move in our out of the way.
    if (!isClearOfEntities(pos)) return false;

    Location location = pos.toLocation(match.getWorld());
    Block block = location.getBlock();
    BlockState newState = location.getBlock().getState();
    material.applyTo(newState);

    BlockRenewEvent event = new BlockRenewEvent(block, newState, this);
    match.callEvent(event); // Our own handler will get this and remove the block from the pool
    if (event.isCancelled()) return false;

    newState.update(true, true);

    if (definition.particles || definition.sound) {
      EFFECTS.blockBreak(location, material);
    }

    return true;
  }
}
