package tc.oc.pgm.util;

import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.material.MaterialData;
import org.bukkit.util.BlockVector;

/** Simple util class to store {@link MaterialData} for a specific block vector. */
public class BlockData {
  public MaterialData data;
  public BlockVector vector;

  public BlockData() {}

  public void set(MaterialData data, BlockVector vector) {
    this.data = data;
    this.vector = vector;
  }

  public Block getBlock(World world, BlockVector offset) {
    return world.getBlockAt(
        vector.getBlockX() + offset.getBlockX(),
        vector.getBlockY() + offset.getBlockY(),
        vector.getBlockZ() + offset.getBlockZ());
  }
}
