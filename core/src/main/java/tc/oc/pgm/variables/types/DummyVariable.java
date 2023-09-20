package tc.oc.pgm.variables.types;

import java.lang.reflect.Array;
import java.util.HashMap;
import java.util.Map;
import org.jetbrains.annotations.Nullable;
import tc.oc.pgm.filters.FilterMatchModule;
import tc.oc.pgm.filters.Filterable;
import tc.oc.pgm.variables.VariableDefinition;

public class DummyVariable<T extends Filterable<?>> extends AbstractVariable<T> {

  private final double def;
  private final Map<T, Double> values;

  // Circular buffer of last additions, head marks next location to replace
  private @Nullable final T[] additions;
  private int head = 0;

  public DummyVariable(VariableDefinition<T> definition, double def, Integer exclusive) {
    super(definition);
    this.def = def;
    this.values = new HashMap<>();
    //noinspection unchecked
    this.additions =
        exclusive == null ? null : (T[]) Array.newInstance(definition.getScope(), exclusive);
  }

  @Override
  protected double getValueImpl(T obj) {
    return values.getOrDefault(obj, def);
  }

  @Override
  protected void setValueImpl(T obj, double value) {
    Double oldVal = values.put(obj, value);

    // Limit is enabled, and we're not replacing a pre-existing key
    if (additions != null && oldVal == null) {
      T toRemove = additions[head];
      if (toRemove != null) {
        values.remove(toRemove);
        toRemove.moduleRequire(FilterMatchModule.class).invalidate(toRemove);
      }

      additions[head] = obj;
      head = (head + 1) % additions.length;
    }

    // For performance reasons, let's avoid launching an event for every variable change
    obj.moduleRequire(FilterMatchModule.class).invalidate(obj);
  }
}
