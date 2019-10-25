package tc.oc.pgm.features;

import tc.oc.xml.InvalidXMLException;
import tc.oc.xml.Node;

public interface FeatureValidation<T extends FeatureDefinition> {
  void validate(T definition, Node node) throws InvalidXMLException;
}
