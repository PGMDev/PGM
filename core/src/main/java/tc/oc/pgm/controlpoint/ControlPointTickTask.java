package tc.oc.pgm.controlpoint;

import java.time.Duration;
import java.util.List;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.match.MatchScope;
import tc.oc.util.TimeUtils;

public class ControlPointTickTask implements Runnable {
  private static final Duration INTERVAL = Duration.ofMillis(100); // milliseconds, two ticks

  private final Match match;
  private final List<ControlPoint> controlPoints;

  public ControlPointTickTask(Match match, List<ControlPoint> controlPoints) {
    this.match = match;
    this.controlPoints = controlPoints;
  }

  public void start() {
    this.match.getScheduler(MatchScope.RUNNING).runTaskTimer(0, TimeUtils.toTicks(INTERVAL), this);
  }

  public void stop() {}

  @Override
  public void run() {
    for (ControlPoint controlPoint : this.controlPoints) {
      controlPoint.tick(INTERVAL);
    }
  }
}
