package tc.oc.pgm.events;

import org.bukkit.event.HandlerList;
import tc.oc.pgm.match.Match;
import tc.oc.pgm.result.VictoryCondition;

/** Fired when the current {@link VictoryCondition} changes */
public class MatchResultChangeEvent extends MatchEvent {
  private final VictoryCondition result;

  public MatchResultChangeEvent(Match match, VictoryCondition result) {
    super(match);
    this.result = result;
  }

  public VictoryCondition getResult() {
    return result;
  }

  private static HandlerList handlers = new HandlerList();

  @Override
  public HandlerList getHandlers() {
    return handlers;
  }

  public static HandlerList getHandlerList() {
    return handlers;
  }
}
