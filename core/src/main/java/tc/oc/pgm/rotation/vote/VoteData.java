package tc.oc.pgm.rotation.vote;

import tc.oc.pgm.rotation.pools.VotingPool;

public class VoteData {
  private final double weight;
  private double score;

  public VoteData(double weight, double score) {
    this.score = score;
    this.weight = weight;
  }

  public void setScore(double score) {
    this.score = score;
  }

  public VoteData withScore(double score) {
    setScore(score);
    return this;
  }

  public double getWeight() {
    return weight;
  }

  public double getScore() {
    return score;
  }

  public void tickScore(VotingPool.VoteConstants constants) {
    this.score = score > constants.defaultScore()
        ? Math.max(score - constants.scoreDecay(), constants.defaultScore())
        : Math.min(score + constants.scoreRise(), constants.defaultScore());
  }
}
