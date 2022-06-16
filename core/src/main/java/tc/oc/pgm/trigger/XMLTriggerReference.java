package tc.oc.pgm.trigger;

import javax.annotation.Nullable;
import tc.oc.pgm.features.FeatureDefinitionContext;
import tc.oc.pgm.features.XMLFeatureReference;
import tc.oc.pgm.util.xml.InvalidXMLException;
import tc.oc.pgm.util.xml.Node;

@SuppressWarnings({"rawtypes", "unchecked"})
public class XMLTriggerReference<T> extends XMLFeatureReference<TriggerDefinition>
    implements Trigger<T> {

  private final Class<? super T> bound;

  public XMLTriggerReference(
      FeatureDefinitionContext context, Node node, @Nullable String id, Class<T> bound) {
    super(context, node, id, TriggerDefinition.class);
    this.bound = bound;
  }

  @Override
  public void resolve() throws InvalidXMLException {
    Node node = this.node; // In case we need to report an error

    super.resolve();

    if (super.referent == null) return;

    Class<?> ref = super.referent.getTriggerType();
    if (!ref.isAssignableFrom(this.bound)) {
      throw new InvalidXMLException(
          "Wrong trigger target for ID '"
              + id
              + "': expected "
              + bound.getSimpleName()
              + " rather than "
              + ref.getSimpleName(),
          node);
    }
  }

  @Override
  public Class<T> getTriggerType() {
    return (Class<T>) get().getTriggerType();
  }

  @Override
  public void trigger(T t) {
    ((Trigger<? super T>) get()).trigger(t);
  }
}
