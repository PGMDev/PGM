package tc.oc.pgm.variables.types;

import tc.oc.pgm.filters.Filterable;
import tc.oc.pgm.variables.Variable;

public interface IndexedVariable<T extends Filterable<?>> extends Variable<T> {

  double getValue(Filterable<?> context, int idx);

  void setValue(Filterable<?> context, int idx, double value);

  int size();
}
