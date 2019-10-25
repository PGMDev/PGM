package tc.oc.pgm.events;

import org.bukkit.event.HandlerList;
import tc.oc.pgm.match.Match;

public class MatchBeginEvent extends MatchEvent {
  private static final HandlerList handlers = new HandlerList();

  public MatchBeginEvent(Match match) {
    super(match);
  }

  @Override
  public HandlerList getHandlers() {
    return handlers;
  }

  public static HandlerList getHandlerList() {
    return handlers;
  }
}
