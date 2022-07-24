package tc.oc.pgm.filters.matcher;

import tc.oc.pgm.api.filter.FilterDefinition;
import tc.oc.pgm.api.filter.query.Query;

/**
 * A filter that NEVER responds to queries outside of {@link #queryType()}, and SOMETIMES responds
 * to queries extending {@link #queryType()}.
 *
 * <p>Queries of the latter type are passed to {@link #queryTyped(Query)}.
 *
 * <p>The runtime type of {@link Q} is detected automatically if it is specified by a subclass.
 */
public interface WeakTypedFilter<Q extends Query> extends FilterDefinition {

  Class<? extends Q> queryType();

  @Override
  default QueryResponse query(Query query) {
    Class<? extends Q> type = queryType();
    return type.isInstance(query) ? queryTyped(type.cast(query)) : QueryResponse.ABSTAIN;
  }

  QueryResponse queryTyped(Q query);
}
