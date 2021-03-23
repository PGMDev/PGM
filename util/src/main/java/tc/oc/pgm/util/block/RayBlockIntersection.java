package tc.oc.pgm.util.block;

import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.util.Vector;

/** Result of a ray-block intersection test */
public class RayBlockIntersection {
  private final Block block;
  private final BlockFace face;
  private final Vector position;

  public RayBlockIntersection(Block block, BlockFace face, Vector position) {
    this.block = block;
    this.face = face;
    this.position = position;
  }

  /** @return The intersected block */
  public Block getBlock() {
    return block;
  }

  /** @return The first intersected face of the block */
  public BlockFace getFace() {
    return face;
  }

  /** @return The first intersected point on the surface of the block */
  public Vector getPosition() {
    return position;
  }
}
