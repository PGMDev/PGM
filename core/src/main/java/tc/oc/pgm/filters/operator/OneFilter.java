package tc.oc.pgm.filters.operator;

import java.util.Collection;
import org.jspecify.annotations.Nullable;
import tc.oc.pgm.api.filter.Filter;
import tc.oc.pgm.api.filter.query.Query;
import tc.oc.pgm.util.math.LocalFrame;

public class OneFilter extends MultiFilterFunction {
  public OneFilter(Iterable<Filter> filters) {
    super(filters);
  }

  @Override
  public QueryResponse query(Query query) {
    return query(query, null);
  }

  @Override
  public QueryResponse query(Query query, @Nullable LocalFrame locals) {
    // returns true if exactly one of the filters match
    boolean hasAllow = false;
    QueryResponse response = QueryResponse.ABSTAIN;
    for (Filter filter : this.filters) {
      QueryResponse filterResponse = filter.query(query, locals);
      if (filterResponse == QueryResponse.ALLOW) {
        if (hasAllow) {
          return QueryResponse.DENY;
        }
        hasAllow = true;
        response = QueryResponse.ALLOW;
      } else if (filterResponse == QueryResponse.DENY && !hasAllow) {
        response = QueryResponse.DENY;
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
