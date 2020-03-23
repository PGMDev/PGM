package tc.oc.pgm.filters;

import java.util.Arrays;
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
    switch (filters.length) {
      case 0:
        return StaticFilter.ABSTAIN;
      case 1:
        return filters[0];
      default:
        return new AnyFilter(Arrays.asList(filters));
    }
  }
}
