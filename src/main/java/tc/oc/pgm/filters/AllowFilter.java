package tc.oc.pgm.filters;

import tc.oc.pgm.filters.query.IQuery;

/**
 * Allow if the child filter allows, otherwise abstain (in other words, transform deny to abstain).
 */
public class AllowFilter extends SingleFilterFunction {

  public AllowFilter(Filter filter) {
    super(filter);
  }

  @Override
  public QueryResponse query(IQuery query) {
    switch (filter.query(query)) {
      case ALLOW:
        return QueryResponse.ALLOW;
      default:
        return QueryResponse.ABSTAIN;
    }
  }
}
