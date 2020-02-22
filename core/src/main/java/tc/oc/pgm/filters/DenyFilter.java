package tc.oc.pgm.filters;

import tc.oc.pgm.filters.query.IQuery;

/** Deny if the child filter allows, otherwise abstain. */
public class DenyFilter extends SingleFilterFunction {

  public DenyFilter(Filter filter) {
    super(filter);
  }

  @Override
  public QueryResponse query(IQuery query) {
    switch (filter.query(query)) {
      case ALLOW:
        return QueryResponse.DENY;
      default:
        return QueryResponse.ABSTAIN;
    }
  }
}
