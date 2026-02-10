package tc.oc.pgm.util.block;

import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.util.Vector;

/**
 * Result of a ray-block intersection test
 *
 * @param block The intersected block
 * @param face The first intersected face of the block
 * @param position The first intersected point on the surface of the block
 */
public record RayBlockIntersection(Block block, BlockFace face, Vector position) {

  /** @return Where a new block would be placed */
  public Block getPlaceAt() {
    return block.getRelative(face);
  }

  @Deprecated
  public Block getBlock() {
    return block();
  }

  @Deprecated
  public BlockFace getFace() {
    return face();
  }

  @Deprecated
  public Vector getPosition() {
    return position();
  }
}
