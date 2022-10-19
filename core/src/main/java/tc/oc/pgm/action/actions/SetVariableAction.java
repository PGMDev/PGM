package tc.oc.pgm.action.actions;

import tc.oc.pgm.filters.Filterable;
import tc.oc.pgm.util.math.Formula;
import tc.oc.pgm.variables.VariableDefinition;

public class SetVariableAction<T extends Filterable<?>> extends AbstractAction<T> {

  private final VariableDefinition<?> variable;
  private final Formula<T> formula;

  public SetVariableAction(Class<T> scope, VariableDefinition<?> variable, Formula<T> formula) {
    super(scope);
    this.variable = variable;
    this.formula = formula;
  }

  @Override
  public void trigger(T t) {
    variable.getVariable(t.getMatch()).setValue(t, formula.applyAsDouble(t));
  }
}
