package tc.oc.pgm.action.actions;

import tc.oc.pgm.filters.Filterable;
import tc.oc.pgm.util.math.Formula;
import tc.oc.pgm.variables.types.LocalVariable;

public class LetAction<T extends Filterable<?>> extends SetVariableAction<T> {

  public LetAction(Class<T> scope, LocalVariable variable, Formula<T> formula) {
    super(scope, variable, formula);
  }

  // Untriggering runs in a fresh frame, so locals must be assigned again
  @Override
  public void untrigger(T t) {
    trigger(t);
  }
}
