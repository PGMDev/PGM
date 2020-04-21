package tc.oc.pgm.controlpoint;

import java.time.Duration;
import java.util.List;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.match.Tickable;
import tc.oc.pgm.api.time.Tick;
import tc.oc.pgm.util.TimeUtils;

public class ControlPointTickTask implements Tickable {
  private final List<ControlPoint> controlPoints;
  private final Duration tick = Duration.ofMillis(TimeUtils.TICK);

  public ControlPointTickTask(List<ControlPoint> controlPoints) {
    this.controlPoints = controlPoints;
  }

  @Override
  public void tick(Match match, Tick tick) {
    for (ControlPoint controlPoint : this.controlPoints) {
      controlPoint.tick(this.tick);
    }
  }
}
