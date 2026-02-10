package tc.oc.pgm.action;

import tc.oc.pgm.api.filter.Filter;
import tc.oc.pgm.filters.Filterable;

public record Trigger<T extends Filterable<?>>(
    Class<T> scope, Filter filter, Action<? super T> action) {
  @Deprecated
  public Class<T> getScope() {
    return scope();
  }

  @Deprecated
  public Filter getFilter() {
    return filter();
  }

  @Deprecated
  public Action<? super T> getAction() {
    return action();
  }
}
