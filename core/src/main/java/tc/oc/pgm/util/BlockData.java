package tc.oc.pgm.util;

import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.material.MaterialData;
import org.bukkit.util.BlockVector;

/** Util class to reference a {@link MaterialData} and location of a block. */
public interface BlockData {

  /**
   * Get the material data for this block. Be aware this causes an allocation, so avoid it when
   * iterating potentially large regions.
   *
   * @return a new material data with proper material type and metadata
   */
  default MaterialData getMaterialData() {
    return new MaterialData(getTypeId(), (byte) getData());
  }

  /**
   * Get the material type id.
   *
   * @return the material type id for this block data.
   */
  int getTypeId();

  /**
   * Get the metadata.
   *
   * @return the metadata for the block data.
   */
  int getData();

  /**
   * Get the current position.
   *
   * @return the position of the block data
   */
  BlockVector getBlockVector();

  /**
   * Get the block in the world this data represents, with an offset.
   *
   * @param world the world to get the block on
   * @param offset the offset from original position
   * @return the block at the current position with the added offset
   */
  default Block getBlock(World world, BlockVector offset) {
    return world.getBlockAt(
        getBlockVector().getBlockX() + offset.getBlockX(),
        getBlockVector().getBlockY() + offset.getBlockY(),
        getBlockVector().getBlockZ() + offset.getBlockZ());
  }
}
