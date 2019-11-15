package tc.oc.pgm.api.match.event;

import org.bukkit.event.HandlerList;
import tc.oc.pgm.api.match.Match;

/**
 * Called when the team size limits are changed for a {@link Match}.
 *
 * @see Match#setMaxPlayers(int)
 */
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
