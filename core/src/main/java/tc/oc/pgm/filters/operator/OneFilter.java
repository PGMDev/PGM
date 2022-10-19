package tc.oc.pgm.filters.operator;

import java.util.Collection;
import tc.oc.pgm.api.filter.Filter;
import tc.oc.pgm.api.filter.query.Query;

public class OneFilter extends MultiFilterFunction {
  public OneFilter(Iterable<Filter> filters) {
    super(filters);
  }

  @Override
  public QueryResponse query(Query query) {
    // returns true if exactly one of the filters match
    QueryResponse response = QueryResponse.ABSTAIN;
    for (Filter filter : this.filters) {
      QueryResponse filterResponse = filter.query(query);
      if (filterResponse == QueryResponse.ALLOW) {
        if (response == QueryResponse.ALLOW) {
          return QueryResponse.DENY;
        } else {
          response = filterResponse;
        }
      } else if (filterResponse == QueryResponse.DENY) {
        response = filterResponse;
      }
    }
    return response;
  }

  public static Filter of(Filter... filters) {
    return MultiFilterFunction.of(OneFilter::new, filters);
  }

  public static Filter of(Collection<Filter> filters) {
    return MultiFilterFunction.of(OneFilter::new, filters);
  }
}
