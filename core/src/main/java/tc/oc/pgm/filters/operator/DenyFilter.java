package tc.oc.pgm.filters.operator;

import org.jspecify.annotations.Nullable;
import tc.oc.pgm.api.filter.Filter;
import tc.oc.pgm.api.filter.query.Query;
import tc.oc.pgm.util.math.LocalFrame;

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
    return query(query, null);
  }

  @Override
  public QueryResponse query(Query query, @Nullable LocalFrame locals) {
    return switch (filter.query(query, locals)) {
      case ALLOW -> QueryResponse.DENY;
      default -> QueryResponse.ABSTAIN;
    };
  }
}
