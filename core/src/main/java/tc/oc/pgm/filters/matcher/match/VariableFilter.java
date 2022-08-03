package tc.oc.pgm.filters.matcher.match;

import com.google.common.collect.Range;
import tc.oc.pgm.filters.Filterable;
import tc.oc.pgm.filters.matcher.TypedFilter;
import tc.oc.pgm.variables.VariableDefinition;

public class VariableFilter<T extends Filterable<?>> implements TypedFilter<T> {

  private final VariableDefinition<T> variable;
  private final Range<Double> values;

  public VariableFilter(VariableDefinition<T> variable, Range<Double> values) {
    this.variable = variable;
    this.values = values;
  }

  @Override
  public boolean isDynamic() {
    // Variables' setValue will always invalidate the filterable directly, no events required
    return true;
  }

  @Override
  public boolean matches(T query) {
    return values.contains(variable.getVariable(query.getMatch()).getValue(query));
  }

  @Override
  public Class<? extends T> queryType() {
    return variable.getScope();
  }
}
