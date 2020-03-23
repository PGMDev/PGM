package tc.oc.pgm.api.filter;

import javax.annotation.Nullable;
import tc.oc.pgm.api.filter.query.Query;

/**
 * Can be registered to listen for changes in the response to particular queries on particular
 * filters. When registered, the listener will be called immediately with null as oldResponse and
 * the current value of the query as newResponse. After that, the listener will only be called when
 * the response has changed.
 */
public interface FilterListener {
  void filterQueryChanged(
      Filter filter,
      Query query,
      @Nullable Filter.QueryResponse oldResponse,
      Filter.QueryResponse newResponse);
}
