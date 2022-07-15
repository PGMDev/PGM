package tc.oc.pgm.filters.matcher;

import tc.oc.pgm.api.filter.FilterDefinition;
import tc.oc.pgm.api.filter.query.Query;

/**
 * A filter that NEVER responds to queries outside of {@link #getQueryType()}, and SOMETIMES
 * responds to queries extending {@link #getQueryType()}.
 *
 * <p>Queries of the latter type are passed to {@link #queryTyped(Query)}.
 *
 * <p>The runtime type of {@link Q} is detected automatically if it is specified by a subclass.
 */
public interface WeakTypedFilter<Q extends Query> extends FilterDefinition {

  @Override
  Class<? extends Q> getQueryType();

  @Override
  default QueryResponse query(Query query) {
    Class<? extends Q> type = getQueryType();
    return type.isInstance(query) ? queryTyped(type.cast(query)) : QueryResponse.ABSTAIN;
  }

  QueryResponse queryTyped(Q query);
}
