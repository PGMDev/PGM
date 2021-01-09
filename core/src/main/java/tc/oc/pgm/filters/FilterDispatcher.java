package tc.oc.pgm.filters;

import java.util.function.Consumer;
import tc.oc.pgm.api.filter.Filter;

public interface FilterDispatcher {

  /**
   * Register the given {@link FilterListener} to receive notifications of changes to the response
   * of the given {@link Filter} to any {@link Filterable} assignable to the given scope.
   *
   * <p>The listener is immediately notified of the current filter response for all existing
   * filterables.
   */
  <F extends Filterable<?>> void onChange(
      Class<F> scope, Filter filter, FilterListener<? super F> listener);

  void onChange(Filter filter, FilterListener<? super Filterable<?>> listener);

  /**
   * Register the given listener to be notified whenever the response of the given {@link Filter}
   * changes from DENY to ALLOW for any {@link Filterable} assignable to the given scope.
   *
   * <p>The listener is immediately notified of any existing filterables that are currently ALLOWed
   * by the filter.
   */
  <F extends Filterable<?>> void onRise(
      Class<F> scope, Filter filter, Consumer<? super F> listener);

  void onRise(Filter filter, Consumer<? super Filterable<?>> listener);

  /**
   * Register the given listener to be notified whenever the response of the given {@link Filter}
   * changes from ALLOW to DENY for any {@link Filterable} assignable to the given scope.
   *
   * <p>The listener is immediately notified of any existing filterables that are currently DENYed
   * by the filter.
   */
  <F extends Filterable<?>> void onFall(
      Class<F> scope, Filter filter, Consumer<? super F> listener);

  void onFall(Filter filter, Consumer<? super Filterable<?>> listener);
}
