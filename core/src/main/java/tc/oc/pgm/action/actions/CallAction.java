package tc.oc.pgm.action.actions;

import com.google.common.collect.ImmutableMap;
import java.util.List;
import org.jspecify.annotations.Nullable;
import tc.oc.pgm.action.ActionContext;
import tc.oc.pgm.action.ControlFlow;
import tc.oc.pgm.action.FunctionDefinition;
import tc.oc.pgm.api.feature.FeatureReference;
import tc.oc.pgm.api.filter.Filter;
import tc.oc.pgm.filters.Filterable;
import tc.oc.pgm.util.math.Formula;
import tc.oc.pgm.variables.VariableTarget;

/** Calls a function with arguments from the caller's frame, optionally storing its result. */
public class CallAction<T extends Filterable<?>> extends AbstractAction<T> {

  private final FeatureReference<FunctionDefinition<T>> function;
  private final ImmutableMap<String, Formula<T>> args;
  private final @Nullable VariableTarget<T> result;
  private final Filter filter;
  private @Nullable List<Formula<T>> orderedArgs;

  public CallAction(
      Class<T> scope,
      FeatureReference<FunctionDefinition<T>> function,
      ImmutableMap<String, Formula<T>> args,
      @Nullable VariableTarget<T> result,
      Filter filter) {
    super(scope);
    this.function = function;
    this.args = args;
    this.result = result;
    this.filter = filter;
  }

  @Override
  public void trigger(T t, ActionContext context) {
    if (!filter.query(context.queryOr(t), context.frame()).isAllowed()) return;

    var fn = function.get();
    if (orderedArgs == null)
      orderedArgs = fn.getParams().stream().map(args::get).toList();

    var frame = fn.createFrame();
    for (int i = 0; i < orderedArgs.size(); i++) {
      frame.setDirect(i, orderedArgs.get(i).apply(t, context.frame()));
    }

    var flow = fn.usesControl() ? new ControlFlow(fn.getDefaultValue()) : null;
    fn.getBody().trigger(t, new ActionContext(context.query(), frame, flow));
    if (result != null) result.set(t, context.frame(), flow.value());
  }
}
