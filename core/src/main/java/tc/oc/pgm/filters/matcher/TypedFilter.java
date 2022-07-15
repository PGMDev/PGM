package tc.oc.pgm.filters.matcher;

import tc.oc.pgm.api.filter.query.Query;

public interface TypedFilter<Q extends Query> extends WeakTypedFilter<Q> {

  default QueryResponse queryTyped(Q query) {
    return QueryResponse.fromBoolean(matches(query));
  }

  boolean matches(Q query);

  @Override
  default QueryResponse query(Query query) {
    Class<? extends Q> type = getQueryType();
    if (type.isInstance(query)) {
      return queryTyped(type.cast(query));
    } else {
      return QueryResponse.ABSTAIN;
    }
  }

  abstract class Impl<Q extends Query> implements TypedFilter<Q> {}
}
