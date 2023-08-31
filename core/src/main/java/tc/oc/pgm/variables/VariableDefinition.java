package tc.oc.pgm.variables;

import java.util.function.Function;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.features.SelfIdentifyingFeatureDefinition;
import tc.oc.pgm.filters.Filterable;

public class VariableDefinition<T extends Filterable<?>> extends SelfIdentifyingFeatureDefinition {

  private final Class<T> scope;
  private final boolean isDynamic;
  private final Function<VariableDefinition<T>, Variable<T>> builder;

  public VariableDefinition(
      String id,
      Class<T> scope,
      boolean isDynamic,
      Function<VariableDefinition<T>, Variable<T>> builder) {
    super(id);
    this.scope = scope;
    this.isDynamic = isDynamic;
    this.builder = builder;
  }

  public Class<T> getScope() {
    return scope;
  }

  public boolean isDynamic() {
    return isDynamic;
  }

  public Variable<T> buildInstance() {
    return builder.apply(this);
  }

  @SuppressWarnings("unchecked")
  public Variable<T> getVariable(Match match) {
    return (Variable<T>) match.getFeatureContext().get(this.getId());
  }
}
