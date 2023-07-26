package tc.oc.pgm.activity;

import java.time.Duration;
import java.time.Instant;

public class TeamTimeRecord {

  private Duration timePlayed;
  private Instant inTime;
  private Instant outTime;

  public TeamTimeRecord() {
    this.timePlayed = Duration.ZERO;
    this.inTime = null;
    this.outTime = null;
  }

  public void end() {
    if (this.inTime != null) {
      this.timePlayed = timePlayed.plus(getActiveSessionDuration());
      this.inTime = null;
      this.outTime = Instant.now();
    }
  }

  public void start() {
    this.inTime = Instant.now();
    this.outTime = null;
  }

  public Duration getTimePlayed() {
    if (outTime != null) return timePlayed;

    // If not ended yet add the current session time up
    return timePlayed.plus(getActiveSessionDuration());
  }

  public Duration getActiveSessionDuration() {
    if (inTime == null || outTime != null) return Duration.ZERO;

    return Duration.between(inTime, Instant.now());
  }
}
