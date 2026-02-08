package tc.oc.pgm.util.block;

import lombok.Getter;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.util.Vector;

/** Result of a ray-block intersection test */
@Getter
public class RayBlockIntersection {
  /** The intersected block */
  private final Block block;
  /** The first intersected face of the block */
  private final BlockFace face;
  /** The first intersected point on the surface of the block */
  private final Vector position;

  public RayBlockIntersection(Block block, BlockFace face, Vector position) {
    this.block = block;
    this.face = face;
    this.position = position;
  }

  /** @return Where a new block would be placed */
  public Block getPlaceAt() {
    return block.getRelative(face);
  }
}
