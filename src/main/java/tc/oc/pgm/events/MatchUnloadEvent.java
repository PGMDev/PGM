package tc.oc.pgm.events;

import org.bukkit.event.HandlerList;
import tc.oc.pgm.match.Match;

/**
 * Fired immediately before a {@link Match} unloads. The match has ended and all players have left,
 * but all modules and features are still loaded.
 */
public class MatchUnloadEvent extends MatchEvent {
  private static HandlerList handlers = new HandlerList();

  public MatchUnloadEvent(Match match) {
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
