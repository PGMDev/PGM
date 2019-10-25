package tc.oc.pgm.filters;

import tc.oc.pgm.filters.query.IQuery;

/** Abstain if the child filter abstains, otherwise return the opposite of the child. */
public class InverseFilter extends SingleFilterFunction {

  public InverseFilter(Filter filter) {
    super(filter);
  }

  @Override
  public QueryResponse query(IQuery query) {
    switch (this.filter.query(query)) {
      case ALLOW:
        return QueryResponse.DENY;
      case DENY:
        return QueryResponse.ALLOW;
      default:
        return QueryResponse.ABSTAIN;
    }
  }
}
