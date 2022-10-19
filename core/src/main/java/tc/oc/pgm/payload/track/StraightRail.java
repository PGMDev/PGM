package tc.oc.pgm.payload.track;

import org.bukkit.block.BlockFace;
import org.bukkit.util.Vector;

class StraightRail implements RailOffset {
  private final BlockFace direction;

  protected StraightRail(BlockFace direction) {
    this.direction = direction;
  }

  @Override
  public Vector getOffset(double progress) {
    // Frame progress between -0.5 and 0.5
    progress -= 0.5;

    return new Vector(direction.getModX(), direction.getModY(), direction.getModZ())
        .multiply(progress)
        .setY(-0.5);
  }

  @Override
  public BlockFace getNext() {
    return direction;
  }

  @Override
  public BlockFace getPrevious() {
    return direction.getOppositeFace();
  }
}
