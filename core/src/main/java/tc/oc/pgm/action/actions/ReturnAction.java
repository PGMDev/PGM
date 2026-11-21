package tc.oc.pgm.action.actions;

import org.jspecify.annotations.Nullable;
import tc.oc.pgm.action.ActionContext;
import tc.oc.pgm.action.ControlFlow;
import tc.oc.pgm.api.filter.Filter;
import tc.oc.pgm.filters.Filterable;
import tc.oc.pgm.util.math.Formula;

/** Returns from the root action or function, with a value when inside a function. */
public class ReturnAction<T extends Filterable<?>> extends AbstractAction<T> {

  private final Filter filter;
  private final @Nullable Formula<T> value;

  public ReturnAction(Class<T> scope, Filter filter, @Nullable Formula<T> value) {
    super(scope);
    this.filter = filter;
    this.value = value;
  }

  @Override
  public void trigger(T t, ActionContext context) {
    if (!filter.query(context.queryOr(t), context.frame()).isAllowed()) return;
    if (context.flow() == null)
      throw new IllegalStateException("Return ran outside of its root action");
    if (value == null) context.flow().signal(ControlFlow.Signal.RETURN);
    else context.flow().returnValue(value.apply(t, context.frame()));
  }
}
