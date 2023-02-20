package tc.oc.pgm.api.match.event;

import org.bukkit.event.HandlerList;
import tc.oc.pgm.api.match.Match;

/**
 * Called after a {@link Match} has completely loaded, and all players have been moved to the new
 * match.
 *
 * @see Match#load()
 */
public class MatchAfterLoadEvent extends MatchEvent {

  public MatchAfterLoadEvent(Match match) {
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
