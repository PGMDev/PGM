package tc.oc.pgm.renewable;

import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.util.BlockVector;
import tc.oc.pgm.regions.Bounds;
import tc.oc.pgm.util.nms.material.MaterialData;
import tc.oc.pgm.util.nms.material.MaterialDataProvider;

/**
 * Array-backed volume of block states (id:data pairs) with fixed size and location. All positions
 * in or out are in world coordinates. Initially filled with air.
 */
public class BlockImage {
  private final World world;
  private final BlockVector origin;
  private final BlockVector size;
  private final Bounds bounds;
  private final int volume;
  private final int[] encodedBlocks;

  public BlockImage(World world, Bounds bounds) {
    this(world, bounds, false);
  }

  public BlockImage(World world, Bounds bounds, boolean keepCounts) {
    this.world = world;
    this.bounds = bounds.clone();
    this.origin = this.bounds.getBlockMin();
    this.size = this.bounds.getBlockSize();
    this.volume = Math.max(0, this.bounds.getBlockVolume());
    this.encodedBlocks = new int[this.volume];
  }

  /** @return The dimensions of this image */
  public BlockVector getSize() {
    return size;
  }

  /** @return The minimum world coordinates mapped to this image */
  public BlockVector getOrigin() {
    return origin;
  }

  /** @return The boundaries of this image in world coordinates */
  public Bounds getBounds() {
    return bounds.clone();
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
  @SuppressWarnings("deprecation")
  public MaterialData get(BlockVector pos) {
    int offset = this.offset(pos);
    return MaterialDataProvider.from(this.encodedBlocks[offset]);
  }

  @SuppressWarnings("deprecation")
  public BlockState getState(BlockVector pos) {
    int offset = this.offset(pos);
    BlockState state = pos.toLocation(this.world).getBlock().getState();
    MaterialDataProvider.from(this.encodedBlocks[offset]).apply(state);
    return state;
  }

  /** Set every block in this image to its current state in the world */
  @SuppressWarnings("deprecation")
  public void save() {
    int offset = 0;
    for (BlockVector v : this.bounds.getBlocks()) {
      Block block = this.world.getBlockAt(v.getBlockX(), v.getBlockY(), v.getBlockZ());
      this.encodedBlocks[offset] = MaterialDataProvider.from(block).encode();
      ++offset;
    }
  }

  /**
   * Copy the block at the given position from the image to the world
   *
   * @param pos Block position in world coordinates
   */
  @SuppressWarnings("deprecation")
  public void restore(BlockVector pos) {
    int offset = this.offset(pos);
    MaterialDataProvider.from(this.encodedBlocks[offset])
        .apply(pos.toLocation(this.world).getBlock(), true);
  }

  public void restore(Block block) {
    int offset = this.offset(block.getLocation().toVector().toBlockVector());
    MaterialDataProvider.from(this.encodedBlocks[offset]).apply(block, true);
  }
}
