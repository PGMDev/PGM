package tc.oc.pgm.features;

import javax.annotation.Nullable;
import tc.oc.xml.InvalidXMLException;
import tc.oc.xml.Node;

/**
 * A named feature reference in an XML document. When {@link #resolve()} is called, the feature is
 * looked up in the {@link FeatureDefinitionContext} passed to the constructor, and an
 * InvalidXMLException is thrown if it's not found there.
 */
public class XMLFeatureReference<T extends FeatureDefinition> implements FeatureReference<T> {
  private final FeatureDefinitionContext context;
  private final Node node;
  private final String id;
  private final Class<T> type;

  private @Nullable T referent;

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
          "Unknown " + this.getTypeName() + " ID '" + id + "'", this.node);
    }
    if (!this.type.isInstance(feature)) {
      throw new InvalidXMLException(
          "Wrong type for ID '"
              + id
              + "': expected "
              + this.getTypeName()
              + " rather than "
              + SelfIdentifyingFeatureDefinition.makeTypeName(feature.getClass()),
          this.node);
    }
    this.referent = this.type.cast(feature);
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
