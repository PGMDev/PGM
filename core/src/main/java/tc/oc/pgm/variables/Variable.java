package tc.oc.pgm.variables;

import tc.oc.pgm.api.feature.Feature;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.filters.Filterable;

public interface Variable<T extends Filterable<?>> extends Feature<VariableDefinition<T>> {

  @Override
  default String getId() {
    return getDefinition().getId();
  }

  double getValue(Filterable<?> context);

  void setValue(Filterable<?> context, double value);

  default void postLoad(Match match) {}
}
