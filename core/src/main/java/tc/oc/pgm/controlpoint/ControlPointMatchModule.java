package tc.oc.pgm.controlpoint;

import java.util.ArrayList;
import java.util.List;
import org.bukkit.event.HandlerList;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.match.MatchModule;
import tc.oc.pgm.api.match.MatchScope;
import tc.oc.pgm.payload.PayloadListener;

public class ControlPointMatchModule implements MatchModule {

  private final Match match;
  private final List<ControlPoint> controlPoints = new ArrayList<>();
  private final ControlPointAnnouncer announcer;
  private final PayloadListener payloadListener;

  public ControlPointMatchModule(Match match, List<ControlPoint> points) {
    this.match = match;
    this.controlPoints.addAll(points);

    this.announcer = new ControlPointAnnouncer(this.match);
    this.payloadListener = new PayloadListener();
    match.addTickable(new ControlPointTickTask(this.controlPoints), MatchScope.RUNNING);
  }

  @Override
  public void load() {
    this.match.addListener(this.announcer, MatchScope.RUNNING);
    this.match.addListener(this.payloadListener, MatchScope.RUNNING);
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
    HandlerList.unregisterAll(this.payloadListener);
  }
}
