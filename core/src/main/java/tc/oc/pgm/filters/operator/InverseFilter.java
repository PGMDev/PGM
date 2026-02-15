package tc.oc.pgm.filters.operator;

import tc.oc.pgm.api.filter.Filter;
import tc.oc.pgm.api.filter.query.Query;

/** Abstain if the child filter abstains, otherwise return the opposite of the child. */
public class InverseFilter extends SingleFilterFunction {

  public InverseFilter(Filter filter) {
    super(filter);
  }

  @Override
  public QueryResponse query(Query query) {
    return switch (this.filter.query(query)) {
      case ALLOW -> QueryResponse.DENY;
      case DENY -> QueryResponse.ALLOW;
      default -> QueryResponse.ABSTAIN;
    };
  }
}
