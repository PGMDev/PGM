package tc.oc.pgm.filters;

import java.util.function.Consumer;
import tc.oc.pgm.api.filter.Filter;
import tc.oc.pgm.api.filter.FilterListener;

public interface FilterDispatcher {

  /**
   * Register the given {@link FilterListener} to receive notifications of changes to the response
   * of the given {@link Filter} to any {@link Filterable} assignable to the given scope.
   *
   * <p>The listener is immediately notified of the current filter response for all existing
   * filterables.
   *
   * @param scope The scope of the filter listener
   * @param filter The filter to listen to
   * @param listener The listener that handles the response
   */
  <F extends Filterable<?>> void onChange(
      Class<F> scope, Filter filter, FilterListener<? super F> listener);

  /**
   * Register the given listener to be notified whenever the response of the given {@link Filter}
   * changes from DENY to ALLOW for any {@link Filterable} assignable to the given scope.
   *
   * <p>The listener is immediately notified of any existing filterables that are currently ALLOWed
   * by the filter.
   *
   * @param scope The scope of the filter listener
   * @param filter The filter to listen to
   * @param listener The listener that handles the response
   */
  <F extends Filterable<?>> void onRise(
      Class<F> scope, Filter filter, Consumer<? super F> listener);

  /**
   * Register the given listener to be notified whenever the response of the given {@link Filter}
   * changes from ALLOW to DENY for any {@link Filterable} assignable to the given scope.
   *
   * <p>The listener is immediately notified of any existing filterables that are currently DENYed
   * by the filter.
   *
   * @param scope The scope of the filter listener
   * @param filter The filter to listen to
   * @param listener The listener that handles the response
   */
  <F extends Filterable<?>> void onFall(
      Class<F> scope, Filter filter, Consumer<? super F> listener);
}
