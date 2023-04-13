package tc.oc.pgm.score;

import tc.oc.pgm.api.filter.Filter;

public class ScoreConfig {
  public final int scoreLimit;
  public final int deathScore;
  public final int killScore;
  public final int mercyLimit;
  public final int mercyLimitMin;
  public final Filter scoreboardFilter;

  public ScoreConfig(
      int scoreLimit,
      int deathScore,
      int killScore,
      int mercyLimit,
      int mercyLimitMin,
      Filter scoreboardFilter) {
    this.scoreLimit = scoreLimit;
    this.deathScore = deathScore;
    this.killScore = killScore;
    this.mercyLimit = mercyLimit;
    this.mercyLimitMin = mercyLimitMin;
    this.scoreboardFilter = scoreboardFilter;
  }
}
