package tc.oc.pgm.filters;

import java.util.Arrays;
import java.util.Collection;
import tc.oc.pgm.api.filter.Filter;
import tc.oc.pgm.api.filter.query.Query;

public class AllFilter extends MultiFilterFunction {

  public AllFilter(Iterable<? extends Filter> filters) {
    super(filters);
  }

  @Override
  public QueryResponse query(Query query) {
    // returns true if all the filters match
    QueryResponse response = QueryResponse.ABSTAIN;
    for (Filter filter : this.filters) {
      QueryResponse filterResponse = filter.query(query);
      if (filterResponse == QueryResponse.DENY) {
        return filterResponse;
      } else if (filterResponse == QueryResponse.ALLOW) {
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
        return new AllFilter(Arrays.asList(filters));
    }
  }

  public static Filter of(Collection<Filter> filters) {
    switch (filters.size()) {
      case 0:
        return StaticFilter.ABSTAIN;
      case 1:
        return filters.iterator().next();
      default:
        return new AllFilter(filters);
    }
  }
}
