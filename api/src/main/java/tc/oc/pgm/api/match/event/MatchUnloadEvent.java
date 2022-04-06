package tc.oc.pgm.api.match.event;

import org.bukkit.event.HandlerList;
import tc.oc.pgm.api.match.Match;

/**
 * Called before a {@link Match} unloads, when no players are left, but features are still loaded.
 *
 * @see Match#unload()
 */
public class MatchUnloadEvent extends MatchEvent {

  public MatchUnloadEvent(Match match) {
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
