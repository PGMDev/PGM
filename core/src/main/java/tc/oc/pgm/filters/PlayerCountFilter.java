package tc.oc.pgm.filters;

import com.google.common.collect.Range;
import java.util.Collection;
import tc.oc.pgm.api.filter.Filter;
import tc.oc.pgm.api.filter.query.MatchQuery;
import tc.oc.pgm.api.player.MatchPlayer;

/** Check if the number of players in a certain context is within a defined range. */
public class PlayerCountFilter extends TypedFilter<MatchQuery> {
  private final Range<Integer> range;
  private final Filter countFilter;

  public PlayerCountFilter(Range<Integer> range, Filter countFilter) {
    this.range = range;
    this.countFilter = countFilter;
  }

  @Override
  public Class<? extends MatchQuery> getQueryType() {
    return MatchQuery.class;
  }

  @Override
  public QueryResponse queryTyped(MatchQuery query) {
    int count = 0;
    Collection<MatchPlayer> allPlayers = query.getMatch().getPlayers();
    int total = allPlayers.size();
    boolean hasUpperBound = range.hasUpperBound();
    boolean hasLowerBound = range.hasLowerBound();

    // Not even enough players to consider checking
    if (hasLowerBound && range.lowerEndpoint() > total) return QueryResponse.DENY;

    for (MatchPlayer player : allPlayers) {
      total--;
      if (this.countFilter.query(player.getQuery()) == QueryResponse.ALLOW) {
        count++;
        // Too high, give up
        if (hasUpperBound && range.upperEndpoint() < count) break;

        // In the range - even if every other player passes the filter we won't go out of bounds
        if (hasUpperBound && range.contains(count) && count + total <= range.upperEndpoint()) break;
      }
    }
    return QueryResponse.fromBoolean(range.contains(count));
  }
}
