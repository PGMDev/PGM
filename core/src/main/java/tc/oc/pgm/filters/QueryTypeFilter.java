package tc.oc.pgm.filters;

import tc.oc.pgm.api.filter.FilterDefinition;
import tc.oc.pgm.api.filter.query.Query;

public class QueryTypeFilter implements FilterDefinition {
  protected final Class<? extends Query> type;

  public QueryTypeFilter(Class<? extends Query> type) {
    this.type = type;
  }

  @Override
  public Class<? extends Query> getQueryType() {
    return Query.class;
  }

  @Override
  public QueryResponse query(Query query) {
    return QueryResponse.fromBoolean(type.isInstance(query));
  }

  @Override
  public String toString() {
    return getClass().getSimpleName() + "{type=" + type.getSimpleName() + "}";
  }
}
