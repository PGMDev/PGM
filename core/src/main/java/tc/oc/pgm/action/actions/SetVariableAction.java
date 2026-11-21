package tc.oc.pgm.action.actions;

import tc.oc.pgm.action.ActionContext;
import tc.oc.pgm.filters.Filterable;
import tc.oc.pgm.util.math.Formula;
import tc.oc.pgm.variables.VariableTarget;

public class SetVariableAction<T extends Filterable<?>> extends AbstractAction<T> {

  private final VariableTarget<T> target;
  private final Formula<T> formula;

  public SetVariableAction(Class<T> scope, VariableTarget<T> target, Formula<T> formula) {
    super(scope);
    this.target = target;
    this.formula = formula;
  }

  @Override
  public void trigger(T t, ActionContext context) {
    target.set(t, context.frame(), formula.apply(t, context.frame()));
  }
}
