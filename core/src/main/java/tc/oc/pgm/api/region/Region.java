package tc.oc.pgm.api.region;

import java.util.Iterator;
import java.util.Random;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.entity.Entity;
import org.bukkit.util.BlockVector;
import org.bukkit.util.Vector;
import tc.oc.pgm.api.filter.Filter;
import tc.oc.pgm.regions.Bounds;

/** Represents an arbitrary region in a Bukkit world. */
public interface Region extends Filter {
  /** Test if the region contains the given point */
  boolean contains(Vector point);

  /** Test if the region contains the given point */
  boolean contains(Location point);

  /** Test if the region contains the center of the given block */
  boolean contains(BlockVector pos);

  /** Test if the region contains the center of the given block */
  boolean contains(Block block);

  /** Test if the region contains the center of the given block */
  boolean contains(BlockState block);

  /** Test if the region contains the given entity */
  boolean contains(Entity entity);

  /** Test if moving from the first point to the second crosses into the region */
  boolean enters(Location from, Location to);

  /** Test if moving from the first point to the second crosses into the region */
  boolean enters(Vector from, Vector to);

  /** Test if moving from the first point to the second crosses out of the region */
  boolean exits(Location from, Location to);

  /** Test if moving from the first point to the second crosses out of the region */
  boolean exits(Vector from, Vector to);

  /** Can this region generate evenly distributed random points? */
  boolean canGetRandom();

  /**
   * Gets a random point contained within this region.
   *
   * @param random Random generator to use.
   * @return Random point within this region.
   * @throws UnsupportedOperationException if this region cannot generate random points
   */
  Vector getRandom(Random random);

  /** Does this region contain a finite number of blocks? */
  boolean isBlockBounded();

  /** @return The smallest cuboid that entirely contains this region */
  Bounds getBounds();

  /**
   * Return true if the region is definitely empty, false if it may or may not be empty. This is
   * just used for optimization, so don't do anything expensive to try and return true.
   */
  boolean isEmpty();

  /**
   * Iterate over all the blocks inside this region.
   *
   * @throws UnsupportedOperationException if the region's blocks are not enumerable
   */
  public Iterator<BlockVector> getBlockVectorIterator();

  public Iterable<BlockVector> getBlockVectors();
}
