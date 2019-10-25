package tc.oc.pgm.events;

import com.google.common.collect.Iterables;
import java.util.Collection;
import javax.annotation.Nullable;
import org.bukkit.event.HandlerList;
import tc.oc.pgm.match.Competitor;
import tc.oc.pgm.match.Match;

public class MatchEndEvent extends MatchEvent {
  private static HandlerList handlers = new HandlerList();

  private final Collection<Competitor> winners;

  public MatchEndEvent(Match match, Collection<Competitor> winners) {
    super(match);
    this.winners = winners;
  }

  public Collection<Competitor> getWinners() {
    return winners;
  }

  // TODO: Get rid of this and make everything handle mutliple winners
  public @Nullable Competitor getWinner() {
    if (this.winners.size() == 1) {
      return Iterables.getOnlyElement(this.winners);
    } else {
      return null;
    }
  }

  @Override
  public HandlerList getHandlers() {
    return handlers;
  }

  public static HandlerList getHandlerList() {
    return handlers;
  }
}
