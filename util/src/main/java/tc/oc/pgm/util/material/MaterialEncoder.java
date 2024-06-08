package tc.oc.pgm.util.material;

import gnu.trove.set.TIntSet;
import gnu.trove.set.hash.TIntHashSet;
import java.util.Collection;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.util.BlockVector;

public interface MaterialEncoder {

  int ENCODED_NULL_MATERIAL = -1;

  /**
   * Encode the given world and metadata to a single integer. The encoding is the same one Mojang
   * uses in various places:
   *
   * <p>typeId + metadata << 12
   */
  static int encodeMaterial(MaterialData material) {
    return material == null ? ENCODED_NULL_MATERIAL : material.encoded();
  }

  static int encodeMaterial(Block block) {
    return MaterialData.from(block).encoded();
  }

  static int encodeMaterial(BlockState block) {
    return MaterialData.from(block).encoded();
  }

  static int encodeMaterial(Location location) {
    return encodeMaterial(location.getBlock());
  }

  static int encodeMaterial(World world, BlockVector pos) {
    return encodeMaterial(world.getBlockAt(pos.getBlockX(), pos.getBlockY(), pos.getBlockZ()));
  }

  static TIntSet encodeMaterialSet(Collection<?> materials) {
    TIntSet set = new TIntHashSet(materials.size());
    for (Object material : materials) {
      if (material instanceof MaterialData) {
        set.add(encodeMaterial((MaterialData) material));
      }
    }
    return set;
  }
}
