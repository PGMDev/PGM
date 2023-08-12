package tc.oc.pgm.stats;

import java.time.Duration;
import java.time.Instant;

public class TeamTimeRecord {

  private Duration timePlayed;
  private Instant inTime;

  public TeamTimeRecord() {
    this.timePlayed = Duration.ZERO;
    this.inTime = null;
  }

  public void startParticipation() {
    if (inTime == null) this.inTime = Instant.now();
  }

  public void endParticipation() {
    if (this.inTime == null) return;

    this.timePlayed = timePlayed.plus(getActiveSessionDuration());
    this.inTime = null;
  }

  public Duration getTimePlayed() {
    // If not ended yet add the current session time up
    return inTime == null ? timePlayed : timePlayed.plus(getActiveSessionDuration());
  }

  public Duration getActiveSessionDuration() {
    return (inTime == null) ? Duration.ZERO : Duration.between(inTime, Instant.now());
  }
}
