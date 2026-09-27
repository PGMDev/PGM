package tc.oc.pgm.filters.operator;

import org.jspecify.annotations.Nullable;
import tc.oc.pgm.api.filter.Filter;
import tc.oc.pgm.api.filter.query.Query;
import tc.oc.pgm.util.math.LocalFrame;

/**
 * Allow if the child filter allows, otherwise abstain (in other words, transform deny to abstain).
 */
public class AllowFilter extends SingleFilterFunction {

  public AllowFilter(Filter filter) {
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
      case ALLOW -> QueryResponse.ALLOW;
      default -> QueryResponse.ABSTAIN;
    };
  }
}
