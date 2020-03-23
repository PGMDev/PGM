package tc.oc.pgm.filters;

import org.joda.time.Duration;
import tc.oc.pgm.api.filter.query.MatchQuery;

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
    return QueryResponse.fromBoolean(!query.getMatch().getDuration().isShorterThan(duration));
  }

  public Duration getTime() {
    return duration;
  }

  @Override
  public int compareTo(TimeFilter o) {
    return duration.compareTo(o.duration);
  }
}
