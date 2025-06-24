package tc.oc.pgm.db;

import java.time.Duration;
import java.time.Instant;
import tc.oc.pgm.api.map.MapData;

abstract class MapDataImpl implements MapData {
  protected final String id;
  protected Instant lastPlayed = Instant.EPOCH;
  protected Duration lastDuration = Duration.ZERO;
  protected double score;

  public MapDataImpl(String id, double defaultScore) {
    this.id = id;
    this.score = defaultScore;
  }

  @Override
  public String getId() {
    return id;
  }

  @Override
  public Instant lastPlayed() {
    return lastPlayed;
  }

  @Override
  public Duration lastDuration() {
    return lastDuration;
  }

  @Override
  public double score() {
    return score;
  }

  @Override
  public void setScore(double score, boolean update) {
    this.score = score;
  }

  @Override
  public int hashCode() {
    return id.hashCode();
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof MapData mapData)) return false;
    return getId().equals(mapData.getId());
  }

  @Override
  public String toString() {
    return "MapDataImpl{" + "id='"
        + id + '\'' + ", lastPlayed="
        + lastPlayed + ", lastDuration="
        + lastDuration + ", score="
        + score + '}';
  }
}
