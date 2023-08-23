package tc.oc.pgm.variables;

import java.util.function.Function;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.filters.Filterable;

public enum VariableType {
  DUMMY(DummyVariable::new, Filterable.class),
  LIVES(BlitzVariable::new, MatchPlayer.class);

  private final Function<VariableDefinition<?>, Variable<?>> supplierFunction;
  private final Class<?>[] supportedScopes;

  VariableType(
      Function<VariableDefinition<? extends Filterable>, Variable<?>> supplierFunction,
      Class<?>... supportedScopes) {
    this.supplierFunction = supplierFunction;
    this.supportedScopes = supportedScopes;
  }

  public boolean supports(Class<?> cls) {
    for (Class<?> supportedScope : supportedScopes) {
      if (supportedScope.isAssignableFrom(cls)) {
        return true;
      }
    }
    return false;
  }

  public Variable<?> buildInstance(VariableDefinition<?> definition) {
    return supplierFunction.apply(definition);
  }
}
