package tc.oc.pgm.util;

import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.util.BlockVector;
import tc.oc.pgm.util.nms.material.MaterialData;
import tc.oc.pgm.util.nms.material.legacy.MaterialDataLegacy;

/** Util class to reference a {@link MaterialDataLegacy} and location of a block. */
public interface BlockData {

  /**
   * Get the material data for this block. Be aware this causes an allocation, so avoid it when
   * iterating potentially large regions.
   *
   * @return a new material data with proper material type and metadata
   */
  MaterialData getMaterialData();

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
