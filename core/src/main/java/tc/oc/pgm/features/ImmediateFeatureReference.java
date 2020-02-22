package tc.oc.pgm.features;

import javax.annotation.Nullable;
import tc.oc.util.xml.InvalidXMLException;
import tc.oc.util.xml.Node;

/** A reference to a FeatureDefinition that is immediately available. */
public class ImmediateFeatureReference<T extends FeatureDefinition> implements FeatureReference<T> {
  private final T feature;

  public ImmediateFeatureReference(T feature) {
    this.feature = feature;
  }

  @Override
  public @Nullable Node getNode() {
    return null;
  }

  @Override
  public boolean isResolved() {
    return true;
  }

  @Override
  public void resolve() throws InvalidXMLException {}

  @Override
  public T get() {
    return this.feature;
  }

  @Override
  public String toString() {
    return this.feature.toString();
  }
}
