package tc.oc.pgm.filters;

import tc.oc.pgm.filters.query.IQuery;

public class QueryTypeFilter implements FilterDefinition {
  protected final Class<? extends IQuery> type;

  public QueryTypeFilter(Class<? extends IQuery> type) {
    this.type = type;
  }

  @Override
  public Class<? extends IQuery> getQueryType() {
    return IQuery.class;
  }

  @Override
  public QueryResponse query(IQuery query) {
    return QueryResponse.fromBoolean(type.isInstance(query));
  }

  @Override
  public String toString() {
    return getClass().getSimpleName() + "{type=" + type.getSimpleName() + "}";
  }
}
