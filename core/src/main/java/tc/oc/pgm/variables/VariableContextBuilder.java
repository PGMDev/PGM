package tc.oc.pgm.variables;

import com.google.common.collect.ImmutableList;
import java.util.Map;
import java.util.stream.Collectors;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.filters.Filterable;
import tc.oc.pgm.util.math.Formula;

public class VariableContextBuilder<T extends Filterable<?>> implements Formula.ContextBuilder<T> {

  private final ImmutableList<VariableDefinition<?>> variables;

  public VariableContextBuilder(ImmutableList<VariableDefinition<?>> variables) {
    this.variables = variables;
  }

  @Override
  public Map<String, Double> getVariables(T scope) {
    Match match = scope.getMatch();
    return variables.stream()
        .collect(
            Collectors.toMap(
                VariableDefinition::getId, vd -> vd.getVariable(match).getValue(scope)));
  }
}
