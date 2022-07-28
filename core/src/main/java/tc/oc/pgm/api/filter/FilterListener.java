package tc.oc.pgm.api.filter;

import tc.oc.pgm.filters.FilterMatchModule;
import tc.oc.pgm.filters.Filterable;

/**
 * Handler of dynamic {@link Filter} events
 *
 * @see FilterMatchModule#onChange(Class, Filter, FilterListener)
 */
@FunctionalInterface
public interface FilterListener<F extends Filterable<?>> {

  /**
   * The callback method.
   *
   * @param filterable the filterable that the filter has a new response for
   * @param response the new response, {@code true} if rise and {@code false} if fall
   */
  void filterQueryChanged(F filterable, boolean response);
}
