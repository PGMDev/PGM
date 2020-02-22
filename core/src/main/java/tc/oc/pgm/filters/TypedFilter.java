package tc.oc.pgm.filters;

import tc.oc.pgm.filters.query.IQuery;

public abstract class TypedFilter<Q extends IQuery> implements FilterDefinition {

  @Override
  public abstract Class<? extends Q> getQueryType();

  protected abstract QueryResponse queryTyped(Q query);

  @Override
  public final QueryResponse query(IQuery query) {
    Class<? extends Q> type = getQueryType();
    if (type.isInstance(query)) {
      return queryTyped(type.cast(query));
    } else {
      return QueryResponse.ABSTAIN;
    }
  }
}
