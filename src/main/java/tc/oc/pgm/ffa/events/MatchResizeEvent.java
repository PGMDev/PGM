package tc.oc.pgm.ffa.events;

import org.bukkit.event.HandlerList;
import tc.oc.pgm.events.MatchEvent;
import tc.oc.pgm.match.Match;

public class MatchResizeEvent extends MatchEvent {

  public MatchResizeEvent(Match match) {
    super(match);
  }

  private static final HandlerList handlers = new HandlerList();

  @Override
  public HandlerList getHandlers() {
    return handlers;
  }

  public static HandlerList getHandlerList() {
    return handlers;
  }
}
