package tc.oc.pgm.api.feature;

import javax.annotation.Nullable;
import tc.oc.pgm.api.xml.InvalidXMLException;
import tc.oc.pgm.api.xml.Node;

/** An indirect reference to a {@link tc.oc.pgm.api.feature.FeatureDefinition} */
public interface FeatureReference<T extends FeatureDefinition> {
  @Nullable
  Node getNode();

  /** Test if this reference has been resolved */
  boolean isResolved();

  /** Try to resolve the referenced feature and throw an InvalidXMLException if this fails. */
  void resolve() throws InvalidXMLException;

  /**
   * Return the referenced feature. Throws an IllegalStateException If the reference has not been
   * resolved yet.
   */
  T get() throws IllegalStateException;
}
