package tc.oc.pgm.api.match.event;

import org.bukkit.event.HandlerList;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.match.MatchPhase;

/**
 * Called when a {@link Match} transitions to {@link MatchPhase#RUNNING}.
 *
 * @see MatchPhaseChangeEvent
 */
public class MatchStartEvent extends MatchEvent {

  public MatchStartEvent(Match match) {
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
