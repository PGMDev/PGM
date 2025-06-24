package tc.oc.pgm.filters.operator;

import tc.oc.pgm.api.filter.Filter;
import tc.oc.pgm.api.filter.query.Query;

/** Deny if the child filter allows, otherwise abstain. */
public class DenyFilter extends SingleFilterFunction {

  public DenyFilter(Filter filter) {
    super(filter);
  }

  @Override
  public boolean respondsTo(Class<? extends Query> queryType) {
    // We can't ever guarantee a response
    return false;
  }

  @Override
  public QueryResponse query(Query query) {
    switch (filter.query(query)) {
      case ALLOW:
        return QueryResponse.DENY;
      default:
        return QueryResponse.ABSTAIN;
    }
  }
}
