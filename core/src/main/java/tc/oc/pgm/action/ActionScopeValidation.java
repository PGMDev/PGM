package tc.oc.pgm.action;

import java.util.HashMap;
import java.util.Map;
import tc.oc.pgm.api.feature.FeatureValidation;
import tc.oc.pgm.util.xml.InvalidXMLException;
import tc.oc.pgm.util.xml.Node;

public class ActionScopeValidation implements FeatureValidation<ActionDefinition<?>> {

  private static final Map<Class<?>, ActionScopeValidation> INSTANCES = new HashMap<>();

  public static ActionScopeValidation of(Class<?> scope) {
    ActionScopeValidation validation = INSTANCES.get(scope);
    if (validation != null) return validation;
    synchronized (INSTANCES) {
      validation = INSTANCES.get(scope);
      if (validation != null) return validation;
      INSTANCES.put(scope, validation = new ActionScopeValidation(scope));
      return validation;
    }
  }

  private final Class<?> scope;

  private ActionScopeValidation(Class<?> scope) {
    this.scope = scope;
  }

  @Override
  public void validate(ActionDefinition<?> definition, Node node) throws InvalidXMLException {
    Class<?> definitionScope = definition.getScope();
    if (!definitionScope.isAssignableFrom(scope))
      throw new InvalidXMLException(
          "Wrong action scope, got "
              + definitionScope.getSimpleName()
              + " but expected "
              + scope.getSimpleName(),
          node);
  }
}
