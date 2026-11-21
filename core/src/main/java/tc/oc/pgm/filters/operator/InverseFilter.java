package tc.oc.pgm.filters.operator;

import org.jspecify.annotations.Nullable;
import tc.oc.pgm.api.filter.Filter;
import tc.oc.pgm.api.filter.query.Query;
import tc.oc.pgm.util.math.LocalFrame;

/** Abstain if the child filter abstains, otherwise return the opposite of the child. */
public class InverseFilter extends SingleFilterFunction {

  public InverseFilter(Filter filter) {
    super(filter);
  }

  @Override
  public QueryResponse query(Query query) {
    return query(query, null);
  }

  @Override
  public QueryResponse query(Query query, @Nullable LocalFrame frame) {
    return switch (this.filter.query(query, frame)) {
      case ALLOW -> QueryResponse.DENY;
      case DENY -> QueryResponse.ALLOW;
      default -> QueryResponse.ABSTAIN;
    };
  }
}
