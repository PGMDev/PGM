package tc.oc.pgm.filters.matcher;

import tc.oc.pgm.api.filter.query.Query;

public class QueryTypeFilter implements TypedFilter<Query> {
  protected final Class<? extends Query> type;

  public QueryTypeFilter(Class<? extends Query> type) {
    this.type = type;
  }

  @Override
  public Class<? extends Query> queryType() {
    return Query.class;
  }

  @Override
  public boolean matches(Query query) {
    return type.isInstance(query);
  }

  @Override
  public String toString() {
    return getClass().getSimpleName() + "{type=" + type.getSimpleName() + "}";
  }
}
