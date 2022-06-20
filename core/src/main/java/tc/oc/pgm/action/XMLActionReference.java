package tc.oc.pgm.action;

import javax.annotation.Nullable;
import tc.oc.pgm.features.FeatureDefinitionContext;
import tc.oc.pgm.features.XMLFeatureReference;
import tc.oc.pgm.util.xml.InvalidXMLException;
import tc.oc.pgm.util.xml.Node;

@SuppressWarnings({"rawtypes", "unchecked"})
public class XMLActionReference<S> extends XMLFeatureReference<ActionDefinition>
    implements Action<S> {

  private final Class<? super S> scope;

  public XMLActionReference(
      FeatureDefinitionContext context, Node node, @Nullable String id, Class<S> scope) {
    super(context, node, id, ActionDefinition.class);
    this.scope = scope;
  }

  @Override
  public void resolve() throws InvalidXMLException {
    Node node = this.node; // In case we need to report an error

    super.resolve();

    if (super.referent == null) return;

    Class<?> ref = super.referent.getScope();
    if (!ref.isAssignableFrom(this.scope)) {
      throw new InvalidXMLException(
          "Wrong trigger target for ID '"
              + id
              + "': expected "
              + scope.getSimpleName()
              + " rather than "
              + ref.getSimpleName(),
          node);
    }
  }

  @Override
  public Class<S> getScope() {
    return (Class<S>) get().getScope();
  }

  @Override
  public void trigger(S s) {
    ((Action<? super S>) get()).trigger(s);
  }

  @Override
  public void untrigger(S s) {
    ((Action<? super S>) get()).untrigger(s);
  }
}
