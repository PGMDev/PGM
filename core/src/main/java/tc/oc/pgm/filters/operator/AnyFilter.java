package tc.oc.pgm.filters.operator;

import java.util.Collection;
import tc.oc.pgm.api.filter.Filter;
import tc.oc.pgm.api.filter.query.Query;

public class AnyFilter extends MultiFilterFunction {

  public AnyFilter(Iterable<? extends Filter> filters) {
    super(filters);
  }

  @Override
  public QueryResponse query(Query query) {
    // returns true if any of the filters match
    QueryResponse response = QueryResponse.ABSTAIN;
    for (Filter filter : this.filters) {
      QueryResponse filterResponse = filter.query(query);
      if (filterResponse == QueryResponse.ALLOW) {
        return filterResponse;
      } else if (filterResponse == QueryResponse.DENY) {
        response = filterResponse;
      }
    }
    return response;
  }

  public static Filter of(Filter... filters) {
    return MultiFilterFunction.of(AnyFilter::new, filters);
  }

  public static Filter of(Collection<Filter> filters) {
    return MultiFilterFunction.of(AnyFilter::new, filters);
  }
}
