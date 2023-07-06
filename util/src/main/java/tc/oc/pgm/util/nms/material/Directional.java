package tc.oc.pgm.util.nms.material;

import org.bukkit.block.BlockFace;

public interface Directional {
  void setFacingDirection(BlockFace direction);

  BlockFace getFacingDirection();
}
