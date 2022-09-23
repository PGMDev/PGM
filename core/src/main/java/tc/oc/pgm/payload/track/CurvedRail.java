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
    BlockFace direction = progress < 0.5 ? from : to;
    // Frame progress between 0.5 -> 0 -> 0.5, but in different directions
    progress = Math.abs(progress - 0.5);
    return new Vector(direction.getModX(), 0, direction.getModZ()).multiply(progress).setY(-0.5);
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
