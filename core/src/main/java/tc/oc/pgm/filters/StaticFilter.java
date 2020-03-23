package tc.oc.pgm.filters;

import tc.oc.pgm.api.filter.FilterDefinition;
import tc.oc.pgm.api.filter.query.Query;

public class StaticFilter implements FilterDefinition {
  protected final QueryResponse response;

  private StaticFilter(QueryResponse response) {
    this.response = response;
  }

  @Override
  public Class<? extends Query> getQueryType() {
    return Query.class;
  }

  @Override
  public QueryResponse query(Query query) {
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
