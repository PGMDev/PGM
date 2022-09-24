package tc.oc.pgm.payload.track;

import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.material.Rails;
import org.bukkit.util.Vector;

public interface RailOffset {

  /**
   * Return a normal-like vector for where the position is for the specific progress.
   *
   * @param progress a number between 0 and 1, for 0% to 100% progress within the block
   * @return A vector with values between -0.5 and 0.5. They represent the offset from center.
   */
  Vector getOffset(double progress);

  BlockFace getNext();

  default Block getNextRail(Block current) {
    BlockFace face = getNext();
    Block next = current.getRelative(face);

    if (!(next.getState().getMaterialData() instanceof Rails))
      next = next.getRelative(BlockFace.DOWN);

    return next;
  }

  BlockFace getPrevious();

  default RailOffset reverse() {
    return new BackwardsRail(this);
  }
}
