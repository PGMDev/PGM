package tc.oc.pgm.variables;

import org.jspecify.annotations.Nullable;
import tc.oc.pgm.filters.Filterable;
import tc.oc.pgm.util.math.Formula;
import tc.oc.pgm.util.math.LocalFrame;
import tc.oc.pgm.util.math.LocalRef;

/** A writable destination for a value: a local, a variable or an index of an array variable. */
@FunctionalInterface
public interface VariableTarget<T> {
  void set(T t, @Nullable LocalFrame frame, double value);

  static <T> VariableTarget<T> local(LocalRef local) {
    return (t, frame, value) -> frame.set(local, value);
  }

  static <T extends Filterable<?>> VariableTarget<T> variable(
      Variable<?> var, @Nullable Formula<T> idx) {
    if ((idx != null) != var.isIndexed())
      throw new IllegalArgumentException("Index must be provided if and only if var is indexed");
    if (idx == null) return (t, frame, value) -> var.setValue(t, value);
    var indexed = (Variable.Indexed<?>) var;
    return (t, frame, value) -> indexed.setValue(t, (int) idx.apply(t, frame), value);
  }
}
