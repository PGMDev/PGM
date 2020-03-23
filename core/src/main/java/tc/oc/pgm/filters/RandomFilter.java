package tc.oc.pgm.filters;

import com.google.common.collect.Range;
import tc.oc.pgm.api.filter.query.MatchQuery;

/**
 * Return a pseudo-random result derived from the query and current tick. It is important that the
 * result is constant for any particular query and tick. This is why all IQuery subclasses must
 * implement hashCode and derive it only from the wrapped object.
 */
public class RandomFilter extends TypedFilter<MatchQuery> {
  protected final Range<Double> chance;

  public RandomFilter(double chance) {
    this(Range.lessThan(chance));
  }

  public RandomFilter(Range<Double> chance) {
    this.chance = chance;
  }

  @Override
  public Class<? extends MatchQuery> getQueryType() {
    return MatchQuery.class;
  }

  @Override
  public QueryResponse queryTyped(MatchQuery query) {
    return QueryResponse.fromBoolean(
        this.chance.contains(query.getMatch().getRandomFromTick(query.hashCode())));
  }
}
