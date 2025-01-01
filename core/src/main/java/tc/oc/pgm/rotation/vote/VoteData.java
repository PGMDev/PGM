package tc.oc.pgm.rotation.vote;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import tc.oc.pgm.api.PGM;
import tc.oc.pgm.api.map.MapData;
import tc.oc.pgm.api.map.MapInfo;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.rotation.pools.VotingPool;

public class VoteData {
  private static final long SECONDS_PER_DAY = Duration.of(1, ChronoUnit.DAYS).toSeconds();

  protected final double weight;
  protected final MapData mapData;

  public VoteData(double weight, MapData mapData) {
    this.weight = weight;
    this.mapData = mapData;
  }

  public static VoteData of(double weight, double score, MapData data, boolean persist) {
    return persist ? new VoteData(weight, data) : new Local(weight, score, data);
  }

  public static VoteData of(double weight, double score, MapInfo map, boolean persist) {
    return of(weight, score, PGM.get().getDatastore().getMapData(map.getId(), score), persist);
  }

  public void setScore(double score) {
    mapData.setScore(score, true);
  }

  public void onMatchEnd(Match match, VotingPool.VoteConstants constants) {
    mapData.saveMatch(match, constants.scoreAfterPlay().apply(match));
  }

  public double getWeight() {
    return weight;
  }

  public double getScore() {
    return mapData.score();
  }

  public boolean isOnCooldown(VotingPool.VoteConstants constants) {
    long duration = mapData.lastDuration().toMinutes();
    if (constants.minCooldown() == -1 || constants.minCooldown() > duration) return false;
    long cooldownSeconds = duration * SECONDS_PER_DAY / constants.minutesPerDay();

    return Instant.now().isAfter(mapData.lastPlayed().plusSeconds(cooldownSeconds));
  }

  public void tickScore(VotingPool.VoteConstants constants) {
    mapData.setScore(constants.tickScore(getScore()), false);
  }

  static class Local extends VoteData {
    private double score;

    public Local(double weight, double score, MapData mapData) {
      super(weight, mapData);
      this.score = score;
    }

    @Override
    public double getScore() {
      return score;
    }

    @Override
    public void setScore(double score) {
      this.score = score;
    }

    @Override
    public void tickScore(VotingPool.VoteConstants constants) {
      this.score = constants.tickScore(score);
    }

    @Override
    public void onMatchEnd(Match match, VotingPool.VoteConstants c) {
      this.score = c.scoreAfterPlay().apply(match);

      // When score isn't persisted, we can be storing a ton of maps all with 0 score.
      // If this data starts being used later, we'll run into no maps available!
      // For this reason, store (non-negative) scores as a low value instead
      mapData.saveMatch(
          match, score < 0 ? score : Math.max(score, c.scoreMinToVote()) + c.scoreRise());
    }
  }
}
