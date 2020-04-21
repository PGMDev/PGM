package tc.oc.pgm.filters;

import java.time.Duration;
import tc.oc.pgm.api.filter.query.MatchQuery;
import tc.oc.pgm.util.TimeUtils;

public class TimeFilter extends TypedFilter<MatchQuery> implements Comparable<TimeFilter> {
  private final Duration duration;

  public TimeFilter(Duration duration) {
    this.duration = duration;
  }

  @Override
  public Class<? extends MatchQuery> getQueryType() {
    return MatchQuery.class;
  }

  @Override
  protected QueryResponse queryTyped(MatchQuery query) {
    return QueryResponse.fromBoolean(
        !TimeUtils.isShorterThan(query.getMatch().getDuration(), duration));
  }

  public Duration getTime() {
    return duration;
  }

  @Override
  public int compareTo(TimeFilter o) {
    return duration.compareTo(o.duration);
  }
}
