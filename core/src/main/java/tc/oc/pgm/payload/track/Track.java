package tc.oc.pgm.payload.track;

import com.google.common.collect.ImmutableList;
import org.bukkit.block.Block;
import org.bukkit.util.BlockVector;
import org.bukkit.util.Vector;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.module.exception.ModuleLoadException;
import tc.oc.pgm.util.block.BlockVectors;

public class Track {

  private final ImmutableList<Rail> track;

  public Track(Match match, BlockVector start) {
    ImmutableList.Builder<Rail> builder = ImmutableList.builder();

    Block nextBlock = start.toLocation(match.getWorld()).getBlock();
    RailOffset nextOffset = RailDirection.of(nextBlock, null);

    if (nextOffset == null) throw new ModuleLoadException("Start must be a rail @ " + start);

    // No rail that way, go the opposite way instead
    if (RailDirection.getRail(nextOffset.getNextRail(nextBlock)) == null)
      nextOffset = nextOffset.reverse();

    while (nextOffset != null) {
      builder.add(new Rail(BlockVectors.position(nextBlock.getState()), nextOffset));

      nextBlock = nextOffset.getNextRail(nextBlock);
      nextOffset = RailDirection.of(nextBlock, nextOffset.getNext());
    }

    this.track = builder.build();
  }

  public Vector getVector(double progress) {
    // Remove 1 from overall size, and shift 0.5 forwards
    // A track with 3 blocks goes from 0.5 to 2.5, because start & end occurs at the center of the
    // block.
    double scaledProgress = (progress * (track.size() - 1)) + 0.5;
    Rail rail = track.get((int) scaledProgress);

    return rail.getOffset(scaledProgress % 1d);
  }

  private static class Rail {
    private final BlockVector position;
    private final RailOffset offset;

    public Rail(BlockVector position, RailOffset offset) {
      this.position = BlockVectors.center(position);
      this.offset = offset;
    }

    public Vector getOffset(double progress) {
      return offset.getOffset(progress).add(position);
    }
  }
}
