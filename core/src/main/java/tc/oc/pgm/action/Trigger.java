package tc.oc.pgm.action;

import lombok.AllArgsConstructor;
import lombok.Getter;
import tc.oc.pgm.api.filter.Filter;
import tc.oc.pgm.filters.Filterable;

@Getter
@AllArgsConstructor
public class Trigger<T extends Filterable<?>> {
  private final Class<T> scope;
  private final Filter filter;
  private final Action<? super T> action;
}
