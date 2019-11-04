package tc.oc.pgm.controlpoint;

import java.util.List;
import org.joda.time.Duration;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.match.MatchScope;

public class ControlPointTickTask implements Runnable {
  private static final Duration INTERVAL = Duration.millis(100); // milliseconds, two ticks

  private final Match match;
  private final List<ControlPoint> controlPoints;

  public ControlPointTickTask(Match match, List<ControlPoint> controlPoints) {
    this.match = match;
    this.controlPoints = controlPoints;
  }

  public void start() {
    this.match.getScheduler(MatchScope.RUNNING).runTaskTimer(0, INTERVAL.getMillis() / 50, this);
  }

  public void stop() {}

  @Override
  public void run() {
    for (ControlPoint controlPoint : this.controlPoints) {
      controlPoint.tick(INTERVAL);
    }
  }
}
