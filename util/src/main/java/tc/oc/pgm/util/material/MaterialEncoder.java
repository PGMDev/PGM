package tc.oc.pgm.util.material;

import gnu.trove.set.TIntSet;
import gnu.trove.set.hash.TIntHashSet;
import java.util.Collection;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.material.MaterialData;
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
    return material == null
        ? ENCODED_NULL_MATERIAL
        : encodeMaterial(material.getItemTypeId(), material.getData());
  }

  static int encodeMaterial(int typeId, byte metadata) {
    return typeId + (((int) metadata) << 12);
  }

  static int encodeMaterial(Block block) {
    return encodeMaterial(block.getTypeId(), block.getData());
  }

  static int encodeMaterial(BlockState block) {
    return encodeMaterial(block.getTypeId(), block.getRawData());
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

  static int decodeTypeId(int encoded) {
    return encoded & 0xfff;
  }

  static byte decodeMetadata(int encoded) {
    return (byte) (encoded >> 12);
  }

  static Material decodeType(int encoded) {
    return Material.getMaterial(decodeTypeId(encoded));
  }

  static MaterialData decodeMaterial(int encoded) {
    if (encoded == ENCODED_NULL_MATERIAL) return null;
    Material material = decodeType(encoded);
    if (material.getData() == MaterialData.class) {
      return new MaterialData(material, decodeMetadata(encoded));
    } else {
      return material.getNewData(decodeMetadata(encoded));
    }
  }
}
