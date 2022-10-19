package tc.oc.pgm.payload.track;

import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.Nullable;

class SlopedRail extends StraightRail {

  public SlopedRail(BlockFace direction) {
    super(direction);
  }

  @Override
  public Vector getOffset(double progress) {
    Vector offset = super.getOffset(progress);
    offset.setY(progress - 0.5);
    return offset;
  }

  @Nullable
  @Override
  public Block getNextRail(Block current) {
    return current.getRelative(getNext()).getRelative(BlockFace.UP);
  }
}
