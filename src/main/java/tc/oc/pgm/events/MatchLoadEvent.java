package tc.oc.pgm.events;

import org.bukkit.event.HandlerList;
import tc.oc.pgm.match.Match;

/** Fired after a {@link Match} has completely loaded but before any players have joined */
public class MatchLoadEvent extends MatchEvent {
  private static HandlerList handlers = new HandlerList();

  public MatchLoadEvent(Match match) {
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
