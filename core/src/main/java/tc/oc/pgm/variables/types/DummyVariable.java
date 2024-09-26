package tc.oc.pgm.variables.types;

import java.lang.reflect.Array;
import java.util.HashMap;
import java.util.Map;
import org.jetbrains.annotations.Nullable;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.features.StateHolder;
import tc.oc.pgm.filters.FilterMatchModule;
import tc.oc.pgm.filters.Filterable;

public class DummyVariable<T extends Filterable<?>> extends AbstractVariable<T>
    implements StateHolder<DummyVariable<T>.Values> {

  private final double def;
  private final Integer exclusive;

  public DummyVariable(Class<T> scope, double def, Integer exclusive) {
    super(scope);
    this.def = def;
    this.exclusive = exclusive;
  }

  @Override
  public boolean isDynamic() {
    return true;
  }

  @Override
  public void load(Match match) {
    match
        .getFeatureContext()
        .registerState(this, exclusive == null ? new Values() : new LimitedValues());
  }

  @Override
  protected double getValueImpl(T obj) {
    return obj.state(this).values.getOrDefault(obj, def);
  }

  @Override
  protected void setValueImpl(T obj, double value) {
    obj.state(this).setValue(obj, value);

    // For performance reasons, let's avoid launching an event for every variable change
    obj.moduleRequire(FilterMatchModule.class).invalidate(obj);
  }

  class Values {
    protected final Map<T, Double> values = new HashMap<>();

    protected void setValue(T obj, double value) {
      values.put(obj, value);
    }
  }

  class LimitedValues extends Values {
    // Circular buffer of last additions, head marks next location to replace
    private final @Nullable T[] additions;
    private int head = 0;

    public LimitedValues() {
      //noinspection unchecked
      this.additions = (T[]) Array.newInstance(getScope(), exclusive);
    }

    protected void setValue(T obj, double value) {
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
    }
  }
}
