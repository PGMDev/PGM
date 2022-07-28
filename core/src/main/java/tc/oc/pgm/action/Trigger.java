package tc.oc.pgm.action;

import tc.oc.pgm.api.filter.Filter;
import tc.oc.pgm.filters.Filterable;

public class Trigger<T extends Filterable<?>> {
  private final Class<T> scope;
  private final Filter filter;
  private final Action<? super T> action;

  public Trigger(Class<T> scope, Filter filter, Action<? super T> action) {
    this.scope = scope;
    this.filter = filter;
    this.action = action;
  }

  public Class<T> getScope() {
    return scope;
  }

  public Filter getFilter() {
    return filter;
  }

  public Action<? super T> getAction() {
    return action;
  }
}
