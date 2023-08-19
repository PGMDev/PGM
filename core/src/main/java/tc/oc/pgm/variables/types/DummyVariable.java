package tc.oc.pgm.variables.types;

import java.util.HashMap;
import java.util.Map;
import tc.oc.pgm.filters.FilterMatchModule;
import tc.oc.pgm.filters.Filterable;
import tc.oc.pgm.variables.VariableDefinition;

public class DummyVariable<T extends Filterable<?>>
    extends AbstractVariable<T, VariableDefinition<T>> {

  private final Map<T, Double> values;

  public DummyVariable(VariableDefinition<T> definition) {
    super(definition);
    this.values = new HashMap<>();
  }

  @Override
  protected double getValueImpl(T obj) {
    return values.computeIfAbsent(obj, k -> definition.getDefault());
  }

  @Override
  protected void setValueImpl(T obj, double value) {
    values.put(obj, value);
    // For performance reasons, let's avoid launching an event for every variable change
    obj.moduleRequire(FilterMatchModule.class).invalidate(obj);
  }
}
