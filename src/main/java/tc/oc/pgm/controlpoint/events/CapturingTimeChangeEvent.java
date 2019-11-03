package tc.oc.pgm.controlpoint.events;

import org.bukkit.event.HandlerList;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.controlpoint.ControlPoint;

public class CapturingTimeChangeEvent extends ControlPointEvent {
  private static final HandlerList handlers = new HandlerList();

  public CapturingTimeChangeEvent(Match match, ControlPoint point) {
    super(match, point);
  }

  @Override
  public HandlerList getHandlers() {
    return handlers;
  }

  public static HandlerList getHandlerList() {
    return handlers;
  }
}
