package tc.oc.pgm.events;

import org.bukkit.event.HandlerList;
import tc.oc.pgm.match.Match;

/**
 * Fires after a {@link Match} has completely loaded and all online players have joined. If cycling
 * from a previous match, it will be completely unloaded when this fires.
 */
public class CycleEvent extends MatchEvent {
  private static final HandlerList handlers = new HandlerList();

  private final Match old;

  public CycleEvent(Match match, Match old) {
    super(match);
    this.old = old;
  }

  public Match getOldMatch() {
    return this.old;
  }

  @Override
  public HandlerList getHandlers() {
    return handlers;
  }

  public static HandlerList getHandlerList() {
    return handlers;
  }
}
