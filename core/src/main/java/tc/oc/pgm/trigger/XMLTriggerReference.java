package tc.oc.pgm.trigger;

import javax.annotation.Nullable;
import tc.oc.pgm.features.FeatureDefinitionContext;
import tc.oc.pgm.features.XMLFeatureReference;
import tc.oc.pgm.util.xml.InvalidXMLException;
import tc.oc.pgm.util.xml.Node;

@SuppressWarnings({"rawtypes", "unchecked"})
public class XMLTriggerReference<S> extends XMLFeatureReference<TriggerDefinition>
    implements Trigger<S> {

  private final Class<? super S> scope;

  public XMLTriggerReference(
      FeatureDefinitionContext context, Node node, @Nullable String id, Class<S> scope) {
    super(context, node, id, TriggerDefinition.class);
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
    ((Trigger<? super S>) get()).trigger(s);
  }

  @Override
  public void untrigger(S s) {
    ((Trigger<? super S>) get()).untrigger(s);
  }
}
