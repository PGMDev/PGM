package tc.oc.pgm.api.match.event;

import org.bukkit.event.HandlerList;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.party.VictoryCondition;

/** Called when the current {@link VictoryCondition} of a {@link Match} changes. */
public class MatchVictoryChangeEvent extends MatchEvent {

  private final VictoryCondition result;

  public MatchVictoryChangeEvent(Match match, VictoryCondition result) {
    super(match);
    this.result = result;
  }

  /**
   * Get the {@link VictoryCondition} that changed.
   *
   * @return The {@link VictoryCondition}.
   */
  public final VictoryCondition getResult() {
    return result;
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
