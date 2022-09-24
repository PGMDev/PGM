package tc.oc.pgm.payload.track;

import org.bukkit.block.BlockFace;
import org.bukkit.util.Vector;

class CurvedRail implements RailOffset {
  private final BlockFace from;
  private final BlockFace to;

  public CurvedRail(BlockFace from, BlockFace to) {
    this.from = from;
    this.to = to;
  }

  @Override
  public Vector getOffset(double progress) {
    double remaining = 1 - progress;

    progress *= 0.5;
    remaining *= 0.5;

    return new Vector(
        remaining * from.getModX() + progress * to.getModX(),
        -0.5,
        remaining * from.getModZ() + progress * to.getModZ());
  }

  @Override
  public BlockFace getNext() {
    return to;
  }

  @Override
  public BlockFace getPrevious() {
    return from;
  }
}
