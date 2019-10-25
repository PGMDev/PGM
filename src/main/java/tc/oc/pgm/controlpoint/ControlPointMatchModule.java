package tc.oc.pgm.controlpoint;

import com.google.common.collect.ImmutableList;
import java.util.ArrayList;
import java.util.List;
import org.bukkit.event.HandlerList;
import tc.oc.pgm.match.Match;
import tc.oc.pgm.match.MatchModule;

public class ControlPointMatchModule extends MatchModule {

  private final List<ControlPoint> controlPoints = new ArrayList<>();
  private final ControlPointTickTask tickTask;
  private final ControlPointAnnouncer announcer;

  public ControlPointMatchModule(Match match, ImmutableList<ControlPoint> points) {
    super(match);

    this.controlPoints.addAll(points);

    this.announcer = new ControlPointAnnouncer(this.match);
    this.tickTask = new ControlPointTickTask(this.match, this.controlPoints);
  }

  @Override
  public void load() {
    super.load();
    this.match.registerEvents(this.announcer);
    for (ControlPoint controlPoint : this.controlPoints) {
      controlPoint.registerEvents();
    }
  }

  @Override
  public void unload() {
    for (ControlPoint controlPoint : this.controlPoints) {
      controlPoint.unregisterEvents();
    }
    HandlerList.unregisterAll(this.announcer);
    super.unload();
  }

  @Override
  public void enable() {
    this.tickTask.start();
  }

  @Override
  public void disable() {
    this.tickTask.stop();
  }
}
