package tc.oc.pgm.filters.operator;

import tc.oc.pgm.api.filter.Filter;
import tc.oc.pgm.api.filter.query.Query;

/**
 * Allow if the child filter allows, otherwise abstain (in other words, transform deny to abstain).
 */
public class AllowFilter extends SingleFilterFunction {

  public AllowFilter(Filter filter) {
    super(filter);
  }

  @Override
  public QueryResponse query(Query query) {
    switch (filter.query(query)) {
      case ALLOW:
        return QueryResponse.ALLOW;
      default:
        return QueryResponse.ABSTAIN;
    }
  }
}
