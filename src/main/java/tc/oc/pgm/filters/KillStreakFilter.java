package tc.oc.pgm.filters;

import com.google.common.collect.BoundType;
import com.google.common.collect.Range;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.filters.query.IPlayerQuery;

public class KillStreakFilter extends ParticipantFilter {
  private final Range<Integer> range;
  private final boolean repeat;

  public KillStreakFilter(Range<Integer> range, boolean repeat) {
    this.range = range;
    this.repeat = repeat;
  }

  @Override
  protected QueryResponse queryPlayer(IPlayerQuery query, MatchPlayer player) {
    int streak = 0; // FIXME: Add back kill streak tracker
    if (this.repeat && streak > 0) {
      int modulo =
          this.range.upperEndpoint() - (this.range.upperBoundType() == BoundType.CLOSED ? 0 : 1);
      streak = 1 + (streak - 1) % modulo;
    }
    return QueryResponse.fromBoolean(this.range.contains(streak));
  }
}
