package tc.oc.pgm.regions;

import static com.google.common.collect.Iterators.*;

import java.util.Iterator;
import java.util.function.Supplier;
import org.bukkit.util.BlockVector;
import org.bukkit.util.Vector;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.region.Region;
import tc.oc.pgm.api.region.RegionDefinition;
import tc.oc.pgm.util.block.BlockVectorSet;

public class Union implements RegionDefinition.Static {
  private final Region[] regions;
  private Supplier<Iterator<BlockVector>> iteratorFactory;

  public Union(Region... regions) {
    this.regions = regions;
  }

  public static Region of(Region... regions) {
    return switch (regions.length) {
      case 0 -> EmptyRegion.INSTANCE;
      case 1 -> regions[0];
      default -> new Union(regions);
    };
  }

  public Region[] getRegions() {
    return regions;
  }

  @Override
  public boolean contains(Vector point) {
    for (Region region : this.regions) {
      if (region.getStatic().contains(point)) {
        return true;
      }
    }
    return false;
  }

  @Override
  public boolean isBlockBounded() {
    for (Region region : this.regions) {
      if (!region.isBlockBounded()) {
        return false;
      }
    }
    return true;
  }

  @Override
  public boolean isStatic() {
    for (Region region : this.regions) {
      if (!region.isStatic()) {
        return false;
      }
    }
    return true;
  }

  @Override
  public boolean isEmpty() {
    for (Region region : this.regions) {
      if (!region.isEmpty()) {
        return false;
      }
    }
    return true;
  }

  @Override
  public Region.Static getStaticImpl(Match match) {
    Region[] regions = new Region[this.regions.length];
    for (int i = 0; i < this.regions.length; i++) {
      regions[i] = this.regions[i].getStatic(match);
    }
    return new Union(regions);
  }

  @Override
  public Bounds getBounds() {
    Bounds bounds = Bounds.empty();
    for (Region region : this.regions) {
      bounds = Bounds.union(bounds, region.getBounds());
    }
    return bounds;
  }

  @Override
  public Iterator<BlockVector> getBlockVectorIterator() {
    if (iteratorFactory == null) iteratorFactory = createIteratorFactory();
    return iteratorFactory.get();
  }

  /**
   * Decides on the best strategy for iterating all blocks in the union. There's 3 possible
   * strategies: - Full-scan: iterate the whole region bounds, and for each block check if
   * this::contains. - Pros: Always correct, no memory overhead. - Cons: is slow! for very disperse
   * cuboids this is very bad. - Child-scan: concatenate iterating each of the children. - Pros:
   * Fastest, no memory overhead. - Cons: if children overlap, you can pass through a block twice.
   * Not acceptable. - Filtered-child-scan: concatenate iterating of each child, but filter by
   * visited. - Pros: Always correct, fast even for disperse cuboids - Cons: requires extra memory
   * to keep track of already visited positions. Ideally whenever possible, child scan is preferred
   * (non-overlapping children). When children overlap, pgm will pick full-scan or filtered child
   * scan based on how much emptiness exists.
   *
   * @return A supplier for iterators over the region
   */
  private Supplier<Iterator<BlockVector>> createIteratorFactory() {
    if (!isStatic()) return this::fullScan;

    // Checking all children are disjoint is O(n²), assume overlap and skip the check if too many.
    boolean disjoint = regions.length < 100;
    var childrenVolume = 0;
    for (int i = 0; i < regions.length; i++) {
      var bounds = regions[i].getBounds();
      childrenVolume += bounds.getBlockVolume();
      for (int j = i + 1; disjoint && j < regions.length; j++) {
        if (!Bounds.disjoint(bounds, regions[j].getBounds())) disjoint = false;
      }
    }

    if (disjoint) return this::childScan;
    if (getBounds().getBlockVolume() < childrenVolume * 5) return this::fullScan;

    final int sumVolume = childrenVolume;
    return () -> {
      var visited = new BlockVectorSet(sumVolume);
      return filter(childScan(), visited::add);
    };
  }

  private Iterator<BlockVector> fullScan() {
    return filter(getBounds().getBlockIterator(), this::contains);
  }

  private Iterator<BlockVector> childScan() {
    return concat(transform(forArray(regions), r -> r.getStatic().getBlockVectorIterator()));
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("Union{regions=[");
    for (Region region : this.regions) {
      sb.append(region.toString()).append(",");
    }
    sb.append("]}");
    return sb.toString();
  }
}
