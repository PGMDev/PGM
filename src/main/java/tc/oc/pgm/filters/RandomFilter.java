package tc.oc.pgm.filters;

import com.google.common.collect.Range;
import tc.oc.pgm.filters.query.IMatchQuery;

/**
 * Return a pseudo-random result derived from the query and current tick. It is important that the
 * result is constant for any particular query and tick. This is why all IQuery subclasses must
 * implement hashCode and derive it only from the wrapped object.
 */
public class RandomFilter extends TypedFilter<IMatchQuery> {
  protected final Range<Double> chance;

  public RandomFilter(double chance) {
    this(Range.lessThan(chance));
  }

  public RandomFilter(Range<Double> chance) {
    this.chance = chance;
  }

  @Override
  public Class<? extends IMatchQuery> getQueryType() {
    return IMatchQuery.class;
  }

  @Override
  public QueryResponse queryTyped(IMatchQuery query) {
    return QueryResponse.fromBoolean(
        this.chance.contains(query.getMatch().getRandomDoubleForTick(query.hashCode())));
  }
}
