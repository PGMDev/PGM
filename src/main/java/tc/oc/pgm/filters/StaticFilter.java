package tc.oc.pgm.filters;

import tc.oc.pgm.filters.query.IQuery;

public class StaticFilter implements FilterDefinition {
  protected final QueryResponse response;

  private StaticFilter(QueryResponse response) {
    this.response = response;
  }

  @Override
  public Class<? extends IQuery> getQueryType() {
    return IQuery.class;
  }

  @Override
  public QueryResponse query(IQuery query) {
    return response;
  }

  @Override
  public String toString() {
    return getClass().getSimpleName() + "{response=" + this.response + "}";
  }

  public static final StaticFilter ALLOW = new StaticFilter(QueryResponse.ALLOW);
  public static final StaticFilter DENY = new StaticFilter(QueryResponse.DENY);
  public static final StaticFilter ABSTAIN = new StaticFilter(QueryResponse.ABSTAIN);
}
