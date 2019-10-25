package tc.oc.pgm.controlpoint.events;

import tc.oc.pgm.controlpoint.ControlPoint;
import tc.oc.pgm.events.MatchEvent;
import tc.oc.pgm.match.Match;

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
