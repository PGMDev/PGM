package tc.oc.pgm.api.match.event;

import org.bukkit.event.HandlerList;
import tc.oc.pgm.api.match.Match;

/**
 * Called after a {@link Match} has completely loaded but before any players have joined.
 *
 * @see Match#load()
 */
public class MatchLoadEvent extends MatchEvent {

  public MatchLoadEvent(Match match) {
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
