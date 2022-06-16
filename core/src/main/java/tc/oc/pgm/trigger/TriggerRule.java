package tc.oc.pgm.trigger;

import tc.oc.pgm.api.filter.Filter;
import tc.oc.pgm.filters.dynamic.Filterable;

public class TriggerRule<T extends Filterable<?>> {
  private final Class<T> scope;
  private final Filter filter;
  private final Trigger<? super T> trigger;

  public TriggerRule(Class<T> scope, Filter filter, Trigger<? super T> trigger) {
    this.scope = scope;
    this.filter = filter;
    this.trigger = trigger;
  }

  public Class<T> getScope() {
    return scope;
  }

  public Filter getFilter() {
    return filter;
  }

  public Trigger<? super T> getTrigger() {
    return trigger;
  }
}
