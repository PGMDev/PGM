package tc.oc.pgm.filters;

import org.joda.time.Duration;
import tc.oc.pgm.filters.query.IMatchQuery;

public class TimeFilter extends TypedFilter<IMatchQuery> implements Comparable<TimeFilter> {
  private final Duration duration;

  public TimeFilter(Duration duration) {
    this.duration = duration;
  }

  @Override
  public Class<? extends IMatchQuery> getQueryType() {
    return IMatchQuery.class;
  }

  @Override
  protected QueryResponse queryTyped(IMatchQuery query) {
    return QueryResponse.fromBoolean(!query.getMatch().getRunningTime().isShorterThan(duration));
  }

  public Duration getTime() {
    return duration;
  }

  @Override
  public int compareTo(TimeFilter o) {
    return duration.compareTo(o.duration);
  }
}
