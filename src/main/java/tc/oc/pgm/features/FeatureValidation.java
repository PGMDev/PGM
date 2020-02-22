package tc.oc.pgm.features;

import tc.oc.util.xml.InvalidXMLException;
import tc.oc.util.xml.Node;

public interface FeatureValidation<T extends FeatureDefinition> {
  void validate(T definition, Node node) throws InvalidXMLException;
}
