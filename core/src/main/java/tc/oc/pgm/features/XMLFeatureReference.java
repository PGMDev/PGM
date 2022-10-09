package tc.oc.pgm.features;

import org.jetbrains.annotations.Nullable;
import tc.oc.pgm.api.feature.FeatureDefinition;
import tc.oc.pgm.api.feature.FeatureReference;
import tc.oc.pgm.util.xml.InvalidXMLException;
import tc.oc.pgm.util.xml.Node;

/**
 * A named feature reference in an XML document. When {@link #resolve()} is called, the feature is
 * looked up in the {@link FeatureDefinitionContext} passed to the constructor, and an
 * InvalidXMLException is thrown if it's not found there.
 */
public class XMLFeatureReference<T extends FeatureDefinition> implements FeatureReference<T> {
  protected final FeatureDefinitionContext context;
  protected final String id;
  protected final Class<T> type;

  // Will only be available prior to resolving and validating.
  // Afterwards it will be cleared to allow node, and the underlying DOM to GC
  protected @Nullable Node node;
  protected @Nullable T referent;

  public XMLFeatureReference(FeatureDefinitionContext context, Node node, Class<T> type) {
    this(context, node, null, type);
  }

  public XMLFeatureReference(
      FeatureDefinitionContext context, Node node, @Nullable String id, Class<T> type) {
    this.context = context;
    this.node = node;
    this.id = id != null ? id : node.getValueNormalize();
    this.type = type;
  }

  /** Get the ID of the referenced feature, as parsed from the XML node */
  public String getId() {
    return this.id;
  }

  /** Get the type of the referenced feature */
  public Class<T> getType() {
    return this.type;
  }

  /** Get the XML node that references the feature */
  @Override
  public Node getNode() {
    final Node node = this.node;
    if (node == null)
      throw new IllegalStateException(
          "Unknown " + this.getTypeName() + " ID '" + id + "' (garbage collected)");
    return node;
  }

  /** Get a readable name for the type of the referenced feature */
  public String getTypeName() {
    return SelfIdentifyingFeatureDefinition.makeTypeName(this.type);
  }

  @Override
  public boolean isResolved() {
    return this.referent != null;
  }

  @Override
  public void resolve() throws InvalidXMLException {
    String id = this.getId();
    FeatureDefinition feature = this.context.get(id, FeatureDefinition.class);
    if (feature == null) {
      throw new InvalidXMLException(
          "Unknown " + this.getTypeName() + " ID '" + id + "'", getNode());
    }
    if (!this.type.isInstance(feature)) {
      throw new InvalidXMLException(
          "Wrong type for ID '"
              + id
              + "': expected "
              + this.getTypeName()
              + " rather than "
              + SelfIdentifyingFeatureDefinition.makeTypeName(feature.getClass()),
          getNode());
    }
    this.referent = this.type.cast(feature);
    this.node = null; // Resolved successfully, no longer need reference to the node
  }

  @Override
  public T get() {
    if (!this.isResolved()) {
      throw new IllegalStateException(
          "Cannot access unresolved " + this.getTypeName() + " '" + this.getId() + "'");
    }
    return this.referent;
  }

  @Override
  public String toString() {
    if (this.isResolved()) {
      return this.get().toString();
    } else {
      return "Unresolved " + this.getTypeName() + " '" + this.getId() + "'";
    }
  }
}
