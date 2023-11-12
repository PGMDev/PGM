package tc.oc.pgm.action.actions;

import tc.oc.pgm.filters.Filterable;
import tc.oc.pgm.util.math.Formula;
import tc.oc.pgm.variables.VariableDefinition;
import tc.oc.pgm.variables.types.IndexedVariable;

public class SetVariableAction<T extends Filterable<?>> extends AbstractAction<T> {

  protected final VariableDefinition<?> variable;
  protected final Formula<T> formula;

  public SetVariableAction(Class<T> scope, VariableDefinition<?> variable, Formula<T> formula) {
    super(scope);
    this.variable = variable;
    this.formula = formula;
  }

  @Override
  public void trigger(T t) {
    variable.getVariable(t.getMatch()).setValue(t, formula.applyAsDouble(t));
  }

  public static class Indexed<T extends Filterable<?>> extends SetVariableAction<T> {

    private final Formula<T> idx;

    public Indexed(
        Class<T> scope, VariableDefinition<?> variable, Formula<T> idx, Formula<T> formula) {
      super(scope, variable, formula);
      this.idx = idx;
    }

    @Override
    @SuppressWarnings("unchecked")
    public void trigger(T t) {
      ((IndexedVariable<T>) variable.getVariable(t.getMatch()))
          .setValue(t, (int) idx.applyAsDouble(t), formula.applyAsDouble(t));
    }
  }
}
