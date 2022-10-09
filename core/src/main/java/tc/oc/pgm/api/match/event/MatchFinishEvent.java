package tc.oc.pgm.api.match.event;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import java.util.Collection;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.Nullable;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.match.MatchPhase;
import tc.oc.pgm.api.party.Competitor;

/**
 * Called when a {@link Match} transitions to {@link MatchPhase#FINISHED}.
 *
 * @see MatchPhaseChangeEvent
 */
public class MatchFinishEvent extends MatchEvent {

  private final Collection<Competitor> winners;

  public MatchFinishEvent(Match match, Collection<Competitor> winners) {
    super(match);
    this.winners = ImmutableList.copyOf(winners);
  }

  /**
   * Get all the {@link Competitor} winners of the {@link Match}.
   *
   * @return All the winners.
   */
  public final Collection<Competitor> getWinners() {
    return winners;
  }

  /** @return Either the first and only winner, or {@code null} if none or multiple winners. */
  @Deprecated
  public final @Nullable Competitor getWinner() {
    if (getWinners().size() == 1) {
      return Iterables.getOnlyElement(getWinners());
    } else {
      return null;
    }
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
