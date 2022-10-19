package tc.oc.pgm.action;

import java.util.HashMap;
import java.util.Map;
import tc.oc.pgm.api.feature.FeatureValidation;
import tc.oc.pgm.util.xml.InvalidXMLException;
import tc.oc.pgm.util.xml.Node;

public class ActionScopeValidation implements FeatureValidation<ActionDefinition<?>> {

  private static final Map<Class<?>, ActionScopeValidation> INSTANCES = new HashMap<>();

  public static ActionScopeValidation of(Class<?> scope) {
    return INSTANCES.computeIfAbsent(scope, ActionScopeValidation::new);
  }

  private final Class<?> scope;

  private ActionScopeValidation(Class<?> scope) {
    this.scope = scope;
  }

  @Override
  public void validate(ActionDefinition<?> definition, Node node) throws InvalidXMLException {
    Class<?> scope = definition.getScope();
    if (!scope.isAssignableFrom(this.scope))
      throw new InvalidXMLException(
          "Wrong action scope, got "
              + scope.getSimpleName()
              + " but expected "
              + scope.getSimpleName(),
          node);
  }
}
