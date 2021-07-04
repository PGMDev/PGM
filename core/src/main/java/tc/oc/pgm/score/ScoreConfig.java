package tc.oc.pgm.score;

public class ScoreConfig {
  public final int scoreLimit;
  public final int deathScore;
  public final int killScore;
  public final int mercyLimit;

  public ScoreConfig(int scoreLimit, int deathScore, int killScore, int mercyLimit) {
    this.scoreLimit = scoreLimit;
    this.deathScore = deathScore;
    this.killScore = killScore;
    this.mercyLimit = mercyLimit;
  }
}
