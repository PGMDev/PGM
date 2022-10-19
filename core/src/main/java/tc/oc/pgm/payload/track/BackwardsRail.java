package tc.oc.pgm.payload.track;

import org.bukkit.block.BlockFace;
import org.bukkit.util.Vector;

public class BackwardsRail implements RailOffset {

  private final RailOffset other;

  public BackwardsRail(RailOffset other) {
    this.other = other;
  }

  @Override
  public Vector getOffset(double progress) {
    return other.getOffset(1 - progress);
  }

  @Override
  public BlockFace getNext() {
    return other.getPrevious();
  }

  @Override
  public BlockFace getPrevious() {
    return other.getNext();
  }

  @Override
  public RailOffset reverse() {
    return other;
  }
}
