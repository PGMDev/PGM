package tc.oc.pgm.destroyable;

import com.google.common.base.Preconditions;
import tc.oc.pgm.goals.Contribution;
import tc.oc.pgm.match.MatchPlayerState;

public class DestroyableContribution extends Contribution {
  private final int blocks;

  public DestroyableContribution(MatchPlayerState player, double percentage, int blocks) {
    super(player, percentage);
    Preconditions.checkArgument(blocks > 0, "blocks must be greater than zero");
    this.blocks = blocks;
  }

  public int getBlocks() {
    return this.blocks;
  }
}
