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

  private final int size;
  private final double def;
  private final Map<T, double[]> values;

  public ArrayVariable(VariableDefinition<T> definition, int size, double def) {
    super(definition);
    this.size = size;
    this.def = def;
    this.values = new HashMap<>();
  }

  public int checkBounds(int idx) {
    if (idx < 0 || idx >= size) {
      PGM.get().getGameLogger().log(Level.SEVERE, String.format(OOB_ERR, idx, getId(), size));
      return 0;
    }
    return idx;
  }

  @Override
  public int size() {
    return size;
  }

  @Override
  public double getValue(Filterable<?> obj, int idx) {
    double[] val = values.get(getAncestor(obj));
    return val == null ? def : val[checkBounds(idx)];
  }

  @Override
  public void setValue(Filterable<?> obj, int idx, double value) {
    values.compute(
        getAncestor(obj),
        (k, arr) -> {
          if (arr == null) {
            if (value == def) return null;
            arr = buildArray();
          }
          arr[checkBounds(idx)] = value;
          return arr;
        });

    // For performance reasons, let's avoid launching an event for every variable change
    obj.moduleRequire(FilterMatchModule.class).invalidate(obj);
  }

  private double[] buildArray() {
    double[] result = new double[size];
    if (def != 0) Arrays.fill(result, def);
    return result;
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
