package tc.oc.pgm.events;

import java.time.Duration;
import org.bukkit.event.HandlerList;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.match.event.MatchEvent;

public class CancelMatchStartCountdownEvent extends MatchEvent {

  private final Duration remaining;

  public CancelMatchStartCountdownEvent(Match match, Duration remaining) {
    super(match);
    this.remaining = remaining;
  }

  public Duration remaining() {
    return remaining;
  }

  private static final HandlerList handlers = new HandlerList();

  public static HandlerList getHandlerList() {
    return handlers;
  }

  @Override
  public HandlerList getHandlers() {
    return handlers;
  }
}
