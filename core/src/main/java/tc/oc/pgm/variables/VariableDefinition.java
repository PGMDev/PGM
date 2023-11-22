package tc.oc.pgm.variables;

import java.util.function.Function;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.features.SelfIdentifyingFeatureDefinition;
import tc.oc.pgm.filters.Filterable;

public class VariableDefinition<T extends Filterable<?>> extends SelfIdentifyingFeatureDefinition {

  private final Class<T> scope;
  private final boolean isDynamic, isIndexed;
  private final Function<VariableDefinition<T>, Variable<T>> builder;

  public VariableDefinition(
      String id,
      Class<T> scope,
      boolean isDynamic,
      boolean isIndexed,
      Function<VariableDefinition<T>, Variable<T>> builder) {
    super(id);
    this.scope = scope;
    this.isDynamic = isDynamic;
    this.isIndexed = isIndexed;
    this.builder = builder;
  }

  public static <T extends Filterable<?>> VariableDefinition<T> ofStatic(
      String id, Class<T> scope, Function<VariableDefinition<T>, Variable<T>> builder) {
    return new VariableDefinition<>(id, scope, false, false, builder);
  }

  public Class<T> getScope() {
    return scope;
  }

  public boolean isDynamic() {
    return isDynamic;
  }

  public boolean isIndexed() {
    return isIndexed;
  }

  public Variable<T> buildInstance() {
    return builder.apply(this);
  }

  @SuppressWarnings("unchecked")
  public Variable<T> getVariable(Match match) {
    return (Variable<T>) match.getFeatureContext().get(this.getId());
  }
}
