package tc.oc.pgm.renewable;

import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.util.BlockVector;
import tc.oc.pgm.regions.Bounds;

/**
 * Array-backed volume of block states with fixed size and location. All positions in or out are in
 * world coordinates. Initially filled with air.
 */
public class BlockImage {
  private final World world;
  private final BlockVector origin;
  private final BlockVector size;
  private final Bounds bounds;
  private final Material[] materials;

  public BlockImage(World world, Bounds bounds) {
    this.world = world;
    this.bounds = bounds.clone();
    this.origin = this.bounds.getBlockMin();
    this.size = this.bounds.getBlockSize();
    int volume = Math.max(0, this.bounds.getBlockVolume());
    this.materials = new Material[volume];
  }

  private int offset(BlockVector pos) {
    if (!this.bounds.containsBlock(pos)) {
      throw new IndexOutOfBoundsException("Block is not inside this BlockImage");
    }

    return (pos.getBlockZ() - this.origin.getBlockZ())
            * this.size.getBlockX()
            * this.size.getBlockY()
        + (pos.getBlockY() - this.origin.getBlockY()) * this.size.getBlockX()
        + (pos.getBlockX() - this.origin.getBlockX());
  }

  /**
   * @param pos Block position in world coordinates
   * @return Block state saved in this image at the given position
   */
  public Material get(BlockVector pos) {
    int offset = this.offset(pos);
    return this.materials[offset];
  }

  /** Set every block in this image to its current state in the world */
  @SuppressWarnings("deprecation")
  public void save() {
    int offset = 0;
    for (BlockVector v : this.bounds.getBlocks()) {
      Block block = this.world.getBlockAt(v.getBlockX(), v.getBlockY(), v.getBlockZ());
      this.materials[offset] = block.getType();
      ++offset;
    }
  }

  /** Copy the block at the given position from the image to the world */
  public void restore(Block block) {
    int offset = this.offset(block.getLocation().toVector().toBlockVector());
    block.setType(this.materials[offset], true);
  }
}
