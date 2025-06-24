package tc.oc.pgm.variables.types;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import tc.oc.pgm.api.PGM;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.features.StateHolder;
import tc.oc.pgm.filters.FilterMatchModule;
import tc.oc.pgm.filters.Filterable;
import tc.oc.pgm.variables.Variable;
import tc.oc.pgm.variables.VariablesMatchModule;

public class ArrayVariable<T extends Filterable<?>> extends AbstractVariable<T>
    implements Variable.Indexed<T>, StateHolder<Map<T, double[]>> {

  private static final String OOB_ERR = "Index %d out of bounds for array '%s' (length %d)";

  private final int size;
  private final double def;

  public ArrayVariable(Class<T> scope, int size, double def) {
    super(scope);
    this.size = size;
    this.def = def;
  }

  @Override
  public boolean isDynamic() {
    return true;
  }

  @Override
  public void load(Match match) {
    match.getFeatureContext().registerState(this, new HashMap<>());
  }

  @Override
  public int size() {
    return size;
  }

  @Override
  public double getValue(Filterable<?> obj, int idx) {
    var scope = getAncestor(obj);
    double[] val = obj.state(this).get(scope);
    return val == null ? def : val[checkBounds(idx, scope)];
  }

  @Override
  public void setValue(Filterable<?> obj, int idx, double value) {
    obj.state(this).compute(getAncestor(obj), (scope, arr) -> {
      if (arr == null) {
        if (value == def) return null;
        arr = buildArray();
      }
      arr[checkBounds(idx, scope)] = value;
      return arr;
    });

    // For performance reasons, let's avoid launching an event for every variable change
    obj.moduleRequire(FilterMatchModule.class).invalidate(obj);
  }

  public int checkBounds(int idx, T obj) {
    if (idx < 0 || idx >= size) {
      String id = obj.moduleRequire(VariablesMatchModule.class).getId(this);
      PGM.get().getGameLogger().log(Level.SEVERE, String.format(OOB_ERR, idx, id, size));
      return 0;
    }
    return idx;
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
