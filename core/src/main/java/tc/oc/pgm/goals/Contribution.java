package tc.oc.pgm.goals;

import static tc.oc.pgm.util.Assert.assertTrue;

import lombok.Getter;
import tc.oc.pgm.api.player.MatchPlayerState;

@Getter
public class Contribution {
  private final MatchPlayerState playerState;
  private final double percentage;

  public Contribution(MatchPlayerState playerState, double percentage) {
    assertTrue(
        percentage > 0 && percentage <= 1,
        "percentage must be greater than zero and less than or equal to 1");
    this.playerState = playerState;
    this.percentage = percentage;
  }
}
