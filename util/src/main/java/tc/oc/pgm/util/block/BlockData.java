package tc.oc.pgm.util.block;

import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.util.BlockVector;
import tc.oc.pgm.util.material.BlockMaterialData;
import tc.oc.pgm.util.material.MaterialData;

/** Util class to reference a {@link BlockMaterialData} and location of a block. */
public interface BlockData extends BlockMaterialData {

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
    if (offset == null) {
      return world.getBlockAt(
          getBlockVector().getBlockX(),
          getBlockVector().getBlockY(),
          getBlockVector().getBlockZ());
    } else {
      return world.getBlockAt(
          getBlockVector().getBlockX() + offset.getBlockX(),
          getBlockVector().getBlockY() + offset.getBlockY(),
          getBlockVector().getBlockZ() + offset.getBlockZ());
    }
  }

  /**
   * Check if this block data has the same material as the block at this position with the given
   * offset in the world.
   *
   * @param world the world to check the block in
   * @param offset the offset from the original position
   * @return true if the materials match
   */
  default boolean sameMaterialAs(World world, BlockVector offset) {
    return encoded() == MaterialData.block(getBlock(world, offset)).encoded();
  }
}
