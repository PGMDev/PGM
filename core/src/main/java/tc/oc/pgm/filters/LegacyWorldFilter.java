package tc.oc.pgm.filters;

import tc.oc.pgm.api.filter.FilterDefinition;
import tc.oc.pgm.api.filter.query.EntityTypeQuery;
import tc.oc.pgm.api.filter.query.Query;

/** Used to implement the legacy "allow-world" and "deny-world" filters */
public class LegacyWorldFilter implements FilterDefinition {

  @Override
  public Class<? extends Query> getQueryType() {
    return Query.class;
  }

  @Override
  public QueryResponse query(Query query) {
    return QueryResponse.fromBoolean(!(query instanceof EntityTypeQuery));
  }
}
