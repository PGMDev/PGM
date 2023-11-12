package tc.oc.pgm.variables.types;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import tc.oc.pgm.api.PGM;
import tc.oc.pgm.filters.FilterMatchModule;
import tc.oc.pgm.filters.Filterable;
import tc.oc.pgm.variables.VariableDefinition;

public class ArrayVariable<T extends Filterable<?>> extends AbstractVariable<T>
    implements IndexedVariable<T> {

  private static final String OOB_ERR = "Index %d out of bounds for array '%s' (length %d)";

  private final double[] def;
  private final Map<T, double[]> values;

  public ArrayVariable(VariableDefinition<T> definition, int size, double def) {
    super(definition);
    Arrays.fill(this.def = new double[size], def);
    this.values = new HashMap<>();
  }

  public int checkBounds(int idx) {
    if (idx < 0 || idx >= def.length) {
      PGM.get().getGameLogger().log(Level.SEVERE, String.format(OOB_ERR, idx, getId(), def.length));
      return 0;
    }
    return idx;
  }

  @Override
  public int size() {
    return def.length;
  }

  @Override
  public double getValue(Filterable<?> obj, int idx) {
    return values.getOrDefault(getAncestor(obj), def)[checkBounds(idx)];
  }

  @Override
  public void setValue(Filterable<?> obj, int idx, double value) {
    values.compute(
        getAncestor(obj),
        (k, v) -> {
          if (v == null) v = Arrays.copyOf(def, def.length);
          v[checkBounds(idx)] = value;
          return v;
        });

    // For performance reasons, let's avoid launching an event for every variable change
    obj.moduleRequire(FilterMatchModule.class).invalidate(obj);
  }

  @Override
  protected double getValueImpl(T obj) {
    throw new UnsupportedOperationException();
  }

  @Override
  protected void setValueImpl(T obj, double value) {
    throw new UnsupportedOperationException();
  }
}
