package tc.oc.pgm.variables;

import java.util.HashMap;
import java.util.Map;
import tc.oc.pgm.api.feature.Feature;
import tc.oc.pgm.filters.FilterMatchModule;
import tc.oc.pgm.filters.Filterable;

public class Variable<T extends Filterable<?>> implements Feature<VariableDefinition<T>> {

  private final VariableDefinition<T> definition;
  private final Map<T, Double> values;

  public Variable(VariableDefinition<T> definition) {
    this.definition = definition;
    this.values = new HashMap<>();
  }

  @Override
  public String getId() {
    return definition.getId();
  }

  @Override
  public VariableDefinition<T> getDefinition() {
    return definition;
  }

  public double getValue(Filterable<?> context) {
    return values.computeIfAbsent(getAncestor(context), k -> definition.getDefault());
  }

  public void setValue(Filterable<?> context, double value) {
    T ctx = getAncestor(context);
    values.put(ctx, value);
    // For performance reasons, let's avoid launching an event for every variable change
    context.getMatch().needModule(FilterMatchModule.class).invalidate(ctx);
  }

  private T getAncestor(Filterable<?> context) {
    T filterable = context.getFilterableAncestor(definition.getScope());
    if (filterable != null) return filterable;

    throw new IllegalStateException(
        "Wrong variable scope for '"
            + getId()
            + "', expected "
            + definition.getScope().getSimpleName()
            + " which cannot be found in "
            + context.getClass().getSimpleName());
  }
}
