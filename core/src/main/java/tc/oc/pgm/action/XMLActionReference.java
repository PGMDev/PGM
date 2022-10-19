package tc.oc.pgm.action;

import org.jetbrains.annotations.Nullable;
import tc.oc.pgm.features.FeatureDefinitionContext;
import tc.oc.pgm.features.XMLFeatureReference;
import tc.oc.pgm.util.xml.Node;

@SuppressWarnings({"rawtypes", "unchecked"})
public class XMLActionReference<S> extends XMLFeatureReference<ActionDefinition>
    implements Action<S> {

  public XMLActionReference(FeatureDefinitionContext context, Node node, @Nullable String id) {
    super(context, node, id, ActionDefinition.class);
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
