package tc.oc.pgm.action.actions;

import com.google.common.collect.ImmutableMap;
import org.jspecify.annotations.Nullable;
import tc.oc.pgm.action.ActionContext;
import tc.oc.pgm.action.FunctionDefinition;
import tc.oc.pgm.api.feature.FeatureReference;
import tc.oc.pgm.api.filter.Filter;
import tc.oc.pgm.filters.Filterable;
import tc.oc.pgm.util.math.Formula;
import tc.oc.pgm.util.math.LocalFrame;

/** Calls a function with arguments from the caller's frame, optionally storing its result. */
public class CallAction<T extends Filterable<?>> extends AbstractAction<T> {

  private final FeatureReference<FunctionDefinition> function;
  private final ImmutableMap<String, Formula<T>> args;
  private final @Nullable Target<T> result;
  private final Filter filter;

  public CallAction(
      Class<T> scope,
      FeatureReference<FunctionDefinition> function,
      ImmutableMap<String, Formula<T>> args,
      @Nullable Target<T> result,
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
    var frame = fn.createFrame();
    for (var param : fn.getParams().entrySet()) {
      frame.set(param.getValue(), args.get(param.getKey()).apply(t, context.frame()));
    }

    var flow = fn.call(t, context, frame);
    if (result != null && flow.halted()) result.set(t, context.frame(), flow.value());
  }

  @FunctionalInterface
  public interface Target<T> {
    void set(T t, @Nullable LocalFrame frame, double value);
  }
}
