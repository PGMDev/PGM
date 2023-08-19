package tc.oc.pgm.variables.types;

import tc.oc.pgm.filters.Filterable;
import tc.oc.pgm.variables.Variable;
import tc.oc.pgm.variables.VariableDefinition;

public abstract class AbstractVariable<T extends Filterable<?>, D extends VariableDefinition<T>>
    implements Variable<T> {
  protected final D definition;

  public AbstractVariable(D definition) {
    this.definition = definition;
  }

  @Override
  public D getDefinition() {
    return definition;
  }

  protected abstract double getValueImpl(T obj);

  protected abstract void setValueImpl(T obj, double value);

  @Override
  public double getValue(Filterable<?> context) {
    return getValueImpl(getAncestor(context));
  }

  @Override
  public void setValue(Filterable<?> context, double value) {
    setValueImpl(getAncestor(context), value);
  }

  protected T getAncestor(Filterable<?> context) {
    T filterable = context.getFilterableAncestor(definition.getScope());
    if (filterable != null) return filterable;

    throw new IllegalStateException(
        "Wrong variable scope for '"
            + getId()
            + "', expected "
            + definition.getScope().getSimpleName()
            + " which cannot be found in "
            + context.getClass().getSimpleName());
  }
}
