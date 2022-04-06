package tc.oc.pgm.api.feature;

import tc.oc.pgm.api.xml.InvalidXMLException;
import tc.oc.pgm.api.xml.Node;

public interface FeatureValidation<T extends FeatureDefinition> {
  void validate(T definition, Node node) throws InvalidXMLException;
}
