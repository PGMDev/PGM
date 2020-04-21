package tc.oc.pgm.renewable;

import java.util.HashMap;
import java.util.Map;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.material.MaterialData;
import org.bukkit.util.BlockVector;
import tc.oc.pgm.regions.Bounds;
import tc.oc.pgm.util.collection.DefaultMapAdapter;

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
  private final short[] blockIds;
  private final byte[] blockData;
  private final Map<MaterialData, Integer> blockCounts;

  public BlockImage(World world, Bounds bounds) {
    this(world, bounds, false);
  }

  public BlockImage(World world, Bounds bounds, boolean keepCounts) {
    this.world = world;
    this.bounds = bounds.clone();
    this.origin = this.bounds.getBlockMin();
    this.size = this.bounds.getBlockSize();
    this.volume = Math.max(0, this.bounds.getBlockVolume());

    blockIds = new short[this.volume];
    blockData = new byte[this.volume];

    if (keepCounts) {
      this.blockCounts = new DefaultMapAdapter<>(new HashMap<MaterialData, Integer>(), 0);
    } else {
      this.blockCounts = null;
    }
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

  public Map<MaterialData, Integer> getBlockCounts() {
    return blockCounts;
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
    return new MaterialData(this.blockIds[offset], this.blockData[offset]);
  }

  @SuppressWarnings("deprecation")
  public BlockState getState(BlockVector pos) {
    int offset = this.offset(pos);
    BlockState state = pos.toLocation(this.world).getBlock().getState();
    state.setTypeId(this.blockIds[offset]);
    state.setRawData(this.blockData[offset]);
    return state;
  }

  /** Set every block in this image to its current state in the world */
  @SuppressWarnings("deprecation")
  public void save() {
    if (this.blockCounts != null) {
      this.blockCounts.clear();
    }

    int offset = 0;
    for (BlockVector v : this.bounds.getBlocks()) {
      Block block = this.world.getBlockAt(v.getBlockX(), v.getBlockY(), v.getBlockZ());
      this.blockIds[offset] = (short) block.getTypeId();
      this.blockData[offset] = block.getData();
      ++offset;

      if (this.blockCounts != null) {
        MaterialData md = block.getState().getData();
        this.blockCounts.put(md, this.blockCounts.get(md) + 1);
      }
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
    pos.toLocation(this.world)
        .getBlock()
        .setTypeIdAndData(this.blockIds[offset], this.blockData[offset], true);
  }

  public void restore(Block block) {
    int offset = this.offset(block.getLocation().toVector().toBlockVector());
    block.setTypeIdAndData(this.blockIds[offset], this.blockData[offset], true);
  }
}
