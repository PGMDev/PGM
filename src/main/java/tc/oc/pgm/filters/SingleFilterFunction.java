package tc.oc.pgm.filters;

import static com.google.common.base.Preconditions.checkNotNull;

import tc.oc.pgm.filters.query.IQuery;

/** A filter that transforms the result of a single child filter */
public abstract class SingleFilterFunction implements FilterDefinition {

  protected final Filter filter;

  public SingleFilterFunction(Filter filter) {
    this.filter = checkNotNull(filter, "filter may not be null");
  }

  @Override
  public Class<? extends IQuery> getQueryType() {
    return filter.getQueryType();
  }

  @Override
  public String toString() {
    return getClass().getSimpleName() + "{filter=" + this.filter + "}";
  }
}
