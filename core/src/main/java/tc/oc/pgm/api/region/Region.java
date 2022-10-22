package tc.oc.pgm.api.region;

import com.google.common.collect.Iterators;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.Random;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.entity.Entity;
import org.bukkit.event.Event;
import org.bukkit.util.BlockVector;
import org.bukkit.util.Vector;
import tc.oc.pgm.api.filter.query.LocationQuery;
import tc.oc.pgm.filters.matcher.TypedFilter;
import tc.oc.pgm.regions.Bounds;
import tc.oc.pgm.util.block.BlockVectors;
import tc.oc.pgm.util.chunk.ChunkVector;
import tc.oc.pgm.util.event.PlayerCoarseMoveEvent;

/** Represents an arbitrary region in a Bukkit world. */
public interface Region extends TypedFilter<LocationQuery> {
  /** Test if the region contains the given point */
  boolean contains(Vector point);

  /** Test if the region contains the given point */
  default boolean contains(Location point) {
    return this.contains(point.toVector());
  }

  /** Test if the region contains the center of the given block */
  default boolean contains(BlockVector blockPos) {
    return this.contains((Vector) BlockVectors.center(blockPos));
  }

  /** Test if the region contains the center of the given block */
  default boolean contains(Block block) {
    return this.contains(BlockVectors.center(block));
  }

  /** Test if the region contains the center of the given block */
  default boolean contains(BlockState block) {
    return this.contains(BlockVectors.center(block));
  }

  /** Test if the region contains the given entity */
  default boolean contains(Entity entity) {
    return this.contains(entity.getLocation().toVector());
  }

  /** Test if the region contains the queried location */
  default boolean contains(LocationQuery query) {
    return this.contains(query.getBlockCenter());
  }

  /** Test if moving from the first point to the second crosses into the region */
  default boolean enters(Location from, Location to) {
    return !this.contains(from) && this.contains(to);
  }

  /** Test if moving from the first point to the second crosses into the region */
  default boolean enters(Vector from, Vector to) {
    return !this.contains(from) && this.contains(to);
  }

  /** Test if moving from the first point to the second crosses out of the region */
  default boolean exits(Location from, Location to) {
    return this.contains(from) && !this.contains(to);
  }

  /** Test if moving from the first point to the second crosses out of the region */
  default boolean exits(Vector from, Vector to) {
    return this.contains(from) && !this.contains(to);
  }

  /** Can this region generate evenly distributed random points? */
  default boolean canGetRandom() {
    return false;
  }

  /**
   * Gets a random point contained within this region.
   *
   * @param random Random generator to use.
   * @return Random point within this region.
   * @throws UnsupportedOperationException if this region cannot generate random points
   */
  default Vector getRandom(Random random) {
    throw new UnsupportedOperationException(
        "Cannot generate a random point in " + this.getClass().getSimpleName());
  }

  /** Does this region contain a finite number of blocks? */
  default boolean isBlockBounded() {
    return false;
  }

  /** @return The smallest cuboid that entirely contains this region */
  Bounds getBounds();

  /**
   * Return true if the region is definitely empty, false if it may or may not be empty. This is
   * just used for optimization, so don't do anything expensive to try and return true.
   */
  default boolean isEmpty() {
    return false;
  }

  @Override
  default Collection<Class<? extends Event>> getRelevantEvents() {
    return Collections.singleton(PlayerCoarseMoveEvent.class);
  }

  @Override
  default Class<? extends LocationQuery> queryType() {
    return LocationQuery.class;
  }

  @Override
  default boolean matches(LocationQuery query) {
    return contains(query);
  }

  /**
   * Iterate over all the blocks inside this region.
   *
   * @throws UnsupportedOperationException if the region's blocks are not enumerable
   */
  default Iterator<BlockVector> getBlockVectorIterator() {
    return Iterators.filter(this.getBounds().getBlockIterator(), this::contains);
  }

  default Iterable<BlockVector> getBlockVectors() {
    return this::getBlockVectorIterator;
  }

  default Stream<BlockVector> getBlockPositions() {
    return StreamSupport.stream(
        Spliterators.spliterator(getBlockVectorIterator(), 0, Spliterator.ORDERED), false);
  }

  default Iterable<Block> getBlocks(World world) {
    return () ->
        Iterators.transform(getBlockVectorIterator(), pos -> BlockVectors.blockAt(world, pos));
  }

  default Stream<ChunkVector> getChunkPositions() {
    final Bounds bounds = getBounds();
    if (!bounds.isBlockFinite()) {
      throw new UnsupportedOperationException(
          "Cannot enumerate chunks in unbounded region type " + getClass().getSimpleName());
    }

    final ChunkVector min = ChunkVector.ofBlock(bounds.getMin()),
        max = ChunkVector.ofBlock(bounds.getBlockMaxInside());
    return IntStream.rangeClosed(min.getChunkX(), max.getChunkX())
        .mapToObj(
            x ->
                IntStream.rangeClosed(min.getChunkZ(), max.getChunkX())
                    .mapToObj(z -> ChunkVector.of(x, z)))
        .flatMap(f -> f);
  }
}
