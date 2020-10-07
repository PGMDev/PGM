package tc.oc.pgm.filters;

import static com.google.common.base.Preconditions.checkNotNull;

import tc.oc.pgm.api.filter.Filter;
import tc.oc.pgm.api.filter.query.Query;

/**
 * Takes a query and modifies it before passing it onto its child filter
 *
 * @param <Q> is the type of query this filter can modify before passing it on to its child filter
 */
public abstract class QueryModifier<Q extends Query> extends TypedFilter<Q> {

  private final Filter filter;

  QueryModifier(Filter filter) {
    this.filter = checkNotNull(filter, "filter may not be null");
  }

  /** Returns a modified {@link Query} */
  protected abstract Q modifyQuery(Q query);

  public abstract Class<? extends Q> getQueryType();

  protected QueryResponse queryTyped(Q query) {
    return filter.query(modifyQuery(query));
  }
}
