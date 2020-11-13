package tc.oc.pgm.filters;

import com.google.common.collect.Range;
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
    for (MatchPlayer player : query.getMatch().getPlayers()) {
      if (this.countFilter.query(player.getQuery()) == QueryResponse.ALLOW) count++;
    }
    return QueryResponse.fromBoolean(range.contains(count));
  }
}
