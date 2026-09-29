package tc.oc.pgm.action.actions;

import org.jspecify.annotations.Nullable;
import tc.oc.pgm.action.Action;
import tc.oc.pgm.action.ActionContext;
import tc.oc.pgm.filters.Filterable;
import tc.oc.pgm.util.math.Formula;
import tc.oc.pgm.util.math.LocalRef;
import tc.oc.pgm.variables.LocalScope;

public class RepeatAction<B extends Filterable<?>> extends AbstractAction<B> {

  protected final Action<? super B> action;
  protected final Formula<B> formula;
  protected final @Nullable LocalScope locals;
  protected final @Nullable LocalRef index;

  public RepeatAction(
      Class<B> scope,
      Action<? super B> action,
      Formula<B> formula,
      @Nullable LocalScope locals,
      @Nullable LocalRef index) {
    super(scope);
    this.action = action;
    this.formula = formula;
    this.locals = locals;
    this.index = index;
  }

  @Override
  public void trigger(B b, ActionContext context) {
    int times = (int) formula.apply(b, context.frame());
    for (int i = 0; i < times; i++) {
      if (locals == null) {
        action.trigger(b, context);
      } else {
        var frame = locals.createFrame(context.frame());
        if (index != null) frame.set(index, i);
        action.trigger(b, context.withFrame(frame));
      }
      if (!context.continueLoop()) return;
    }
  }
}
