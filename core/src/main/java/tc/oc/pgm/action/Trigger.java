package tc.oc.pgm.action;

import lombok.Getter;
import tc.oc.pgm.api.filter.Filter;
import tc.oc.pgm.filters.Filterable;

@Getter
public class Trigger<T extends Filterable<?>> {
  private final Class<T> scope;
  private final Filter filter;
  private final Action<? super T> action;

  public Trigger(Class<T> scope, Filter filter, Action<? super T> action) {
    this.scope = scope;
    this.filter = filter;
    this.action = action;
  }
}
