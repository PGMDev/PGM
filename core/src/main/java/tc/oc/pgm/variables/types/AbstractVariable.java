package tc.oc.pgm.variables.types;

import tc.oc.pgm.filters.Filterable;
import tc.oc.pgm.variables.Variable;

abstract class AbstractVariable<T extends Filterable<?>> implements Variable<T> {
  private final Class<T> scope;

  public AbstractVariable(Class<T> scope) {
    this.scope = scope;
  }

  public Class<T> getScope() {
    return scope;
  }

  @Override
  public double getValue(Filterable<?> context) {
    return getValueImpl(getAncestor(context));
  }

  @Override
  public void setValue(Filterable<?> context, double value) {
    setValueImpl(getAncestor(context), value);
  }

  protected abstract double getValueImpl(T obj);

  protected abstract void setValueImpl(T obj, double value);

  protected T getAncestor(Filterable<?> context) {
    T filterable = context.getFilterableAncestor(getScope());
    if (filterable != null) return filterable;

    throw new IllegalStateException("Wrong variable scope for '"
        + this
        + "', expected "
        + getScope().getSimpleName()
        + " which cannot be found in "
        + context.getClass().getSimpleName());
  }
}
