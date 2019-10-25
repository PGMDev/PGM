package tc.oc.pgm.filters;

import tc.oc.pgm.filters.query.IEntityTypeQuery;
import tc.oc.pgm.filters.query.IQuery;

/** Used to implement the legacy "allow-world" and "deny-world" filters */
public class LegacyWorldFilter implements FilterDefinition {

  @Override
  public Class<? extends IQuery> getQueryType() {
    return IQuery.class;
  }

  @Override
  public QueryResponse query(IQuery query) {
    return QueryResponse.fromBoolean(!(query instanceof IEntityTypeQuery));
  }
}
