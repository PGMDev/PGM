package tc.oc.pgm.variables;

import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.features.SelfIdentifyingFeatureDefinition;
import tc.oc.pgm.filters.Filterable;

public class VariableDefinition<T extends Filterable<?>> extends SelfIdentifyingFeatureDefinition {

  private final Class<T> scope;
  private final double def;

  public VariableDefinition(String id, Class<T> scope, double def) {
    super(id);
    this.scope = scope;
    this.def = def;
  }

  public Class<T> getScope() {
    return scope;
  }

  public double getDefault() {
    return def;
  }

  @SuppressWarnings("unchecked")
  public Variable<T> getVariable(Match match) {
    return (Variable<T>) match.getFeatureContext().get(this.getId());
  }
}
