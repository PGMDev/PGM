package tc.oc.pgm.filters.matcher.entity;

import tc.oc.pgm.api.filter.query.EntityTypeQuery;
import tc.oc.pgm.api.filter.query.Query;
import tc.oc.pgm.filters.matcher.TypedFilter;

/** Used to implement the legacy "allow-world" and "deny-world" filters */
public class LegacyWorldFilter extends TypedFilter.Impl<Query> {

  @Override
  public Class<? extends Query> queryType() {
    return Query.class;
  }

  @Override
  public boolean matches(Query query) {
    return !(query instanceof EntityTypeQuery);
  }
}
