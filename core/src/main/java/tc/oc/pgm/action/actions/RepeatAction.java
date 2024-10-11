package tc.oc.pgm.action.actions;

import tc.oc.pgm.action.Action;
import tc.oc.pgm.filters.Filterable;
import tc.oc.pgm.util.math.Formula;

public class RepeatAction<B extends Filterable<?>> extends AbstractAction<B> {

  protected final Action<? super B> action;
  protected final Formula<B> formula;

  public RepeatAction(Class<B> scope, Action<? super B> action, Formula<B> formula) {
    super(scope);
    this.action = action;
    this.formula = formula;
  }

  @Override
  public void trigger(B t) {
    for (int i = (int) formula.applyAsDouble(t); i > 0; i--) {
      action.trigger(t);
    }
  }
}
