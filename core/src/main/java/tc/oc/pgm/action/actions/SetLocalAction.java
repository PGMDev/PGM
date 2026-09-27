package tc.oc.pgm.action.actions;

import tc.oc.pgm.action.ActionContext;
import tc.oc.pgm.filters.Filterable;
import tc.oc.pgm.util.math.Formula;
import tc.oc.pgm.util.math.LocalRef;

public class SetLocalAction<T extends Filterable<?>> extends AbstractAction<T> {

  private final LocalRef local;
  private final Formula<T> formula;

  public SetLocalAction(Class<T> scope, LocalRef local, Formula<T> formula) {
    super(scope);
    this.local = local;
    this.formula = formula;
  }

  @Override
  public void trigger(T t, ActionContext context) {
    context.frame().set(local, formula.apply(t, context.frame()));
  }
}
