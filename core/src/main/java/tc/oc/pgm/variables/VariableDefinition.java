package tc.oc.pgm.variables;

import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.features.SelfIdentifyingFeatureDefinition;
import tc.oc.pgm.filters.Filterable;

public class VariableDefinition<T extends Filterable<?>> extends SelfIdentifyingFeatureDefinition {

  private final Class<T> scope;
  private final double def;
  private final VariableType variableType;

  public VariableDefinition(String id, Class<T> scope, double def, VariableType type) {
    super(id);
    this.scope = scope;
    this.def = def;
    this.variableType = type;
  }

  public Class<T> getScope() {
    return scope;
  }

  public double getDefault() {
    return def;
  }

  public Variable<?> buildInstance() {
    return getVariableType().buildInstance(this);
  }

  public VariableType getVariableType() {
    return variableType;
  }

  @SuppressWarnings("unchecked")
  public Variable<T> getVariable(Match match) {
    return (Variable<T>) match.getFeatureContext().get(this.getId());
  }

  public static class Context<T extends Filterable<?>, C> extends VariableDefinition<T> {
    private final C context;

    public Context(String id, Class<T> scope, double def, VariableType type, C context) {
      super(id, scope, def, type);
      this.context = context;
    }

    public C getContext() {
      return context;
    }
  }
}
