package tc.oc.pgm.filters.operator;

import java.util.Collection;
import org.jspecify.annotations.Nullable;
import tc.oc.pgm.api.filter.Filter;
import tc.oc.pgm.api.filter.query.Query;
import tc.oc.pgm.util.math.LocalFrame;

public class AllFilter extends MultiFilterFunction {

  public AllFilter(Iterable<? extends Filter> filters) {
    super(filters);
  }

  @Override
  public QueryResponse query(Query query, @Nullable LocalFrame frame) {
    // returns true if all the filters match
    QueryResponse response = QueryResponse.ABSTAIN;
    for (Filter filter : this.filters) {
      QueryResponse filterResponse = filter.query(query, frame);
      if (filterResponse == QueryResponse.DENY) {
        return filterResponse;
      } else if (filterResponse == QueryResponse.ALLOW) {
        response = filterResponse;
      }
    }
    return response;
  }

  public static Filter of(Filter... filters) {
    return MultiFilterFunction.of(AllFilter::new, filters);
  }

  public static Filter of(Collection<Filter> filters) {
    return MultiFilterFunction.of(AllFilter::new, filters);
  }
}
