package tc.oc.pgm.filters;

import com.google.common.collect.BoundType;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.common.collect.Range;
import java.util.Collection;
import org.bukkit.event.Event;
import tc.oc.pgm.api.filter.Filter;
import tc.oc.pgm.api.filter.query.MatchQuery;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.events.PlayerPartyChangeEvent;

public class PlayerCountFilter extends TypedFilter<MatchQuery> {
  private final Filter filter;
  private final int min, max;

  public PlayerCountFilter(
      Filter filter, Range<Integer> range, boolean participants, boolean observers) {
    if (!observers) filter = AllFilter.of(ParticipatingFilter.PARTICIPATING, filter);
    if (!participants) filter = AllFilter.of(ParticipatingFilter.OBSERVING, filter);
    this.filter = filter;
    this.min =
        !range.hasLowerBound()
            ? Integer.MIN_VALUE
            : range.lowerEndpoint() + (range.lowerBoundType() == BoundType.CLOSED ? 0 : 1);
    this.max =
        !range.hasUpperBound()
            ? Integer.MAX_VALUE
            : range.upperEndpoint() - (range.upperBoundType() == BoundType.CLOSED ? 0 : 1);
  }

  @Override
  public Collection<Class<? extends Event>> getRelevantEvents() {
    return ImmutableList.copyOf(
        Iterables.concat(
            filter.getRelevantEvents(), ImmutableList.of(PlayerPartyChangeEvent.class)));
  }

  @Override
  public Class<? extends MatchQuery> getQueryType() {
    return MatchQuery.class;
  }

  @Override
  protected QueryResponse queryTyped(MatchQuery query) {
    Collection<MatchPlayer> allPlayers = query.getMatch().getPlayers();

    int allowed = 0, potentialMax = allPlayers.size();
    for (MatchPlayer player : allPlayers) {
      if (allowed > max || potentialMax < min) return QueryResponse.DENY;
      if (allowed >= min && potentialMax >= max) return QueryResponse.ALLOW;

      if (filter.query(player).isAllowed()) allowed++;
      else potentialMax--;
    }
    return QueryResponse.fromBoolean(allowed >= min && allowed <= max);
  }
}
