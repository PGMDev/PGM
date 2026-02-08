package tc.oc.pgm.destroyable;

import static tc.oc.pgm.util.Assert.assertTrue;

import lombok.Getter;
import tc.oc.pgm.api.player.MatchPlayerState;
import tc.oc.pgm.goals.Contribution;

@Getter
public class DestroyableContribution extends Contribution {
  private final int blocks;

  public DestroyableContribution(MatchPlayerState player, double percentage, int blocks) {
    super(player, percentage);
    assertTrue(blocks > 0, "blocks must be greater than zero");
    this.blocks = blocks;
  }
}
