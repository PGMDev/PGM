package tc.oc.pgm.regions;

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.material.MaterialData;
import org.bukkit.util.BlockVector;
import org.bukkit.util.Vector;
import tc.oc.block.BlockVectors;
import tc.oc.material.matcher.SingleMaterialMatcher;
import tc.oc.pgm.filters.AnyFilter;
import tc.oc.pgm.filters.BlockFilter;
import tc.oc.pgm.filters.Filter;
import tc.oc.pgm.filters.query.BlockQuery;
import tc.oc.util.SemanticVersion;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;
import java.util.Set;

import static tc.oc.pgm.api.map.MapProtos.REGION_FIX_VERSION;

/**
 * Region represented by a list of single blocks. This will check if a point is inside the block at
 * all.
 */
public class FiniteBlockRegion extends AbstractRegion {
  private final List<Block> blocks;
  private final Bounds bounds;
  private final Set<MaterialData> materials;

  public FiniteBlockRegion(Block... blocks) {
    this(ImmutableList.copyOf(blocks));
  }

  public FiniteBlockRegion(List<Block> blocks) {
    this.blocks = ImmutableList.copyOf(blocks);

    // calculate AABB
    Vector min = new Vector(Double.MAX_VALUE, Double.MAX_VALUE, Double.MAX_VALUE);
    Vector max = new Vector(-Double.MAX_VALUE, -Double.MAX_VALUE, -Double.MAX_VALUE);
    Vector half = new Vector(0.5, 0.5, 0.5);

    ImmutableSet.Builder<MaterialData> materialsBuilder = ImmutableSet.builder();

    for (Block block : this.blocks) {
      Vector center = BlockVectors.center(block).toVector();
      min = Vector.getMinimum(min, center.clone().subtract(half));
      max = Vector.getMaximum(max, center.add(half)); // mutates, but disposed

      materialsBuilder.add(block.getState().getMaterialData());
    }

    this.bounds = new Bounds(min, max);
    this.materials = materialsBuilder.build();
  }

  @Override
  public boolean contains(Vector point) {
    if (!this.bounds.contains(point)) {
      return false;
    }

    Location blockLocation = new Location(null, 0, 0, 0);

    for (Block block : this.blocks) {
      block.getLocation(blockLocation);
      if (BlockVectors.isInside(point, blockLocation)) {
        return true;
      }
    }

    return false;
  }

  @Override
  public boolean contains(Block block) {
    return this.blocks.contains(block);
  }

  @Override
  public boolean contains(BlockState block) {
    return this.contains(block.getBlock());
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
    Block randomBlock = this.blocks.get(random.nextInt(this.blocks.size()));
    double dx = random.nextDouble();
    double dy = random.nextDouble();
    double dz = random.nextDouble();
    return randomBlock.getLocation().add(dx, dy, dz).toVector();
  }

  public List<Block> getBlocks() {
    return this.blocks;
  }

  public Set<MaterialData> getMaterials() {
    return materials;
  }

  @Override
  public String toString() {
    return "FiniteBlockRegion{blocks=[" + Joiner.on(',').join(this.blocks) + "]}";
  }

  public static FiniteBlockRegion fromWorld(
      Region region, World world, @Nullable SemanticVersion proto, SingleMaterialMatcher... materials) {
    return fromWorld(region, world, Arrays.asList(materials), proto);
  }

  public static FiniteBlockRegion fromWorld(
      Region region, World world, Collection<SingleMaterialMatcher> materials, @Nullable SemanticVersion proto) {
    List<Filter> filters = new ArrayList<>(materials.size());
    for (SingleMaterialMatcher materialPattern : materials) {
      filters.add(new BlockFilter(materialPattern));
    }
    return fromWorld(region, world, new AnyFilter(filters), proto);
  }

  @SuppressWarnings("deprecation")
  public static FiniteBlockRegion fromWorld(Region region, World world, Filter materials, @Nullable SemanticVersion proto) {
    List<Block> blocks = new LinkedList<>();
    Bounds bounds = region.getBounds();

    if (region instanceof CuboidRegion
        && proto != null
        && proto.isOlderThan(REGION_FIX_VERSION)) {
      // The loops below are incorrect, because they go one block over the max.
      // Unfortunately, we have to keep this around to avoid breaking old maps.
      Vector min = bounds.getMin();
      Vector max = bounds.getMax();
      for (int x = min.getBlockX(); x <= max.getBlockX(); x++) {
        for (int z = min.getBlockZ(); z <= max.getBlockZ(); z++) {
          for (int y = min.getBlockY(); y <= max.getBlockY(); y++) {
            Block block = world.getBlockAt(x, y, z);
            if (materials.query(new BlockQuery(block)).isAllowed()) {
              blocks.add(block);
            }
          }
        }
      }
    } else {
      // Support for non-cuboid regions was added after the bug was discovered, so
      // we can assume that these regions do not depend on it and handle them correctly.
      for (BlockVector pos : bounds.getBlocks()) {
        Block block = pos.toLocation(world).getBlock();
        if (region.contains(block) && materials.query(new BlockQuery(block)).isAllowed()) {
          blocks.add(block);
        }
      }
    }

    return new FiniteBlockRegion(blocks);
  }
}
