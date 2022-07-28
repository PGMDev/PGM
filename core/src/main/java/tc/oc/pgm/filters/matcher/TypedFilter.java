package tc.oc.pgm.filters.matcher;

import tc.oc.pgm.api.filter.query.Query;

public interface TypedFilter<Q extends Query> extends WeakTypedFilter<Q> {

  @Override
  default boolean respondsTo(Class<? extends Query> queryType) {
    return queryType().isAssignableFrom(queryType);
  }

  default QueryResponse queryTyped(Q query) {
    return QueryResponse.fromBoolean(matches(query));
  }

  boolean matches(Q query);

  abstract class Impl<Q extends Query> implements TypedFilter<Q> {}
}
