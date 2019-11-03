package tc.oc.pgm.controlpoint.events;

import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.match.event.MatchEvent;
import tc.oc.pgm.controlpoint.ControlPoint;

public abstract class ControlPointEvent extends MatchEvent {
  protected final ControlPoint controlPoint;

  public ControlPointEvent(Match match, ControlPoint controlPoint) {
    super(match);
    this.controlPoint = controlPoint;
  }

  public ControlPoint getControlPoint() {
    return this.controlPoint;
  }
}
