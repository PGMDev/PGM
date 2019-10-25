package tc.oc.pgm.controlpoint;

import java.util.List;
import org.bukkit.scheduler.BukkitTask;
import org.joda.time.Duration;
import tc.oc.pgm.PGM;
import tc.oc.pgm.match.Match;

public class ControlPointTickTask implements Runnable {
  private static final Duration INTERVAL = Duration.millis(100); // milliseconds, two ticks

  private final Match match;
  private final List<ControlPoint> controlPoints;
  private BukkitTask task;

  public ControlPointTickTask(Match match, List<ControlPoint> controlPoints) {
    this.match = match;
    this.controlPoints = controlPoints;
  }

  public void start() {
    this.task =
        this.match
            .getServer()
            .getScheduler()
            .runTaskTimer(PGM.get(), this, 0, INTERVAL.getMillis() / 50);
  }

  public void stop() {
    if (this.task != null) {
      this.task.cancel();
    }
  }

  @Override
  public void run() {
    for (ControlPoint controlPoint : this.controlPoints) {
      controlPoint.tick(INTERVAL);
    }
  }
}
