package tc.oc.pgm.filters;

import tc.oc.pgm.api.filter.FilterDefinition;
import tc.oc.pgm.api.filter.query.Query;

public abstract class TypedFilter<Q extends Query> implements FilterDefinition {

  @Override
  public abstract Class<? extends Q> getQueryType();

  protected abstract QueryResponse queryTyped(Q query);

  @Override
  public final QueryResponse query(Query query) {
    Class<? extends Q> type = getQueryType();
    if (type.isInstance(query)) {
      return queryTyped(type.cast(query));
    } else {
      return QueryResponse.ABSTAIN;
    }
  }
}
