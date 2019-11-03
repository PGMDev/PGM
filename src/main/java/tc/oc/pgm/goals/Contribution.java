package tc.oc.pgm.goals;

import com.google.common.base.Preconditions;
import tc.oc.pgm.api.player.MatchPlayerState;

public class Contribution {
  private final MatchPlayerState player;
  private final double percentage;

  public Contribution(MatchPlayerState player, double percentage) {
    Preconditions.checkArgument(
        percentage > 0 && percentage <= 1,
        "percentage must be greater than zero and less than or equal to 1");
    this.player = player;
    this.percentage = percentage;
  }

  public MatchPlayerState getPlayerState() {
    return player;
  }

  public double getPercentage() {
    return percentage;
  }
}
