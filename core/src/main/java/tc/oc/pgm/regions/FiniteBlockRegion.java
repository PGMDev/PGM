package tc.oc.pgm.regions;

import static tc.oc.pgm.api.map.MapProtos.REGION_FIX_VERSION;

import com.google.common.base.Joiner;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.util.BlockVector;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.Nullable;
import tc.oc.pgm.api.filter.Filter;
import tc.oc.pgm.api.region.Region;
import tc.oc.pgm.api.region.RegionDefinition;
import tc.oc.pgm.filters.matcher.block.BlockFilter;
import tc.oc.pgm.filters.operator.AnyFilter;
import tc.oc.pgm.filters.query.BlockQuery;
import tc.oc.pgm.util.Version;
import tc.oc.pgm.util.block.BlockVectorSet;
import tc.oc.pgm.util.block.BlockVectors;
import tc.oc.pgm.util.material.matcher.SingleMaterialMatcher;

/**
 * Region represented by a list of single blocks. This will check if a point is inside the block at
 * all.
 */
public class FiniteBlockRegion implements RegionDefinition {
  private final BlockVectorSet positions;
  private final Bounds bounds;

  public FiniteBlockRegion(Collection<BlockVector> blocks) {
    this.positions = BlockVectorSet.of(blocks);

    // calculate AABB
    Vector min = new Vector(Double.MAX_VALUE, Double.MAX_VALUE, Double.MAX_VALUE);
    Vector max = new Vector(-Double.MAX_VALUE, -Double.MAX_VALUE, -Double.MAX_VALUE);

    for (BlockVector pos : this.positions) {
      min.setX(Math.min(min.getX(), pos.getBlockX()));
      min.setY(Math.min(min.getY(), pos.getBlockY()));
      min.setZ(Math.min(min.getZ(), pos.getBlockZ()));

      max.setX(Math.max(max.getX(), pos.getBlockX() + 1));
      max.setY(Math.max(max.getY(), pos.getBlockY() + 1));
      max.setZ(Math.max(max.getZ(), pos.getBlockZ() + 1));
    }

    this.bounds = new Bounds(min, max);
  }

  @Override
  public boolean contains(Vector point) {
    return bounds.contains(point) && positions.contains(point.toBlockVector());
  }

  @Override
  public boolean canGetRandom() {
    return true;
  }

  @Override
  public boolean isBlockBounded() {
    return true;
  }

  @Override
  public Bounds getBounds() {
    return this.bounds;
  }

  @Override
  public Vector getRandom(Random random) {
    BlockVector randomBlock = this.positions.chooseRandom(random);
    // Calling set avoids allocating a new vector
    randomBlock.setX(randomBlock.getX() + random.nextDouble());
    randomBlock.setY(randomBlock.getY() + random.nextDouble());
    randomBlock.setZ(randomBlock.getZ() + random.nextDouble());
    return randomBlock;
  }

  @Override
  public Iterator<BlockVector> getBlockVectorIterator() {
    return positions.iterator();
  }

  @Override
  public Iterable<BlockVector> getBlockVectors() {
    return positions;
  }

  @Override
  public Stream<BlockVector> getBlockPositions() {
    return positions.stream();
  }

  public int getBlockVolume() {
    return positions.size();
  }

  @Override
  public String toString() {
    return "FiniteBlockRegion{blocks=[" + Joiner.on(',').join(this.positions) + "]}";
  }

  public static FiniteBlockRegion fromWorld(
      Region region, World world, @Nullable Version proto, SingleMaterialMatcher... materials) {
    return fromWorld(region, world, Arrays.asList(materials), proto);
  }

  public static FiniteBlockRegion fromWorld(
      Region region,
      World world,
      Collection<SingleMaterialMatcher> materials,
      @Nullable Version proto) {
    List<Filter> filters = new ArrayList<>(materials.size());
    for (SingleMaterialMatcher materialPattern : materials) {
      filters.add(new BlockFilter(materialPattern));
    }
    return fromWorld(region, world, AnyFilter.of(filters), proto);
  }

  public static FiniteBlockRegion fromWorld(
      Region region, World world, Filter filter, @Nullable Version proto) {
    return fromWorld(
        region, world, block -> filter.query(new BlockQuery(block)).isAllowed(), proto);
  }

  public static FiniteBlockRegion fromWorld(
      Region region, World world, Predicate<Block> filter, @Nullable Version proto) {

    if (region instanceof CuboidRegion && proto != null && proto.isOlderThan(REGION_FIX_VERSION)) {
      // Due to an old bug, legacy maps have cuboids that are one block too big
      Bounds bounds = region.getBounds();
      region = new CuboidRegion(bounds.getMin(), bounds.getMax().add(new Vector(1, 1, 1)));
    }

    return new FiniteBlockRegion(
        region
            .getBlockPositions()
            .filter(pos -> filter.test(BlockVectors.blockAt(world, pos)))
            .collect(Collectors.toCollection(BlockVectorSet::new)));
  }
}
