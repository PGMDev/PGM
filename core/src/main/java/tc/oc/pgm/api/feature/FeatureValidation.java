package tc.oc.pgm.api.feature;

import tc.oc.pgm.util.xml.InvalidXMLException;
import tc.oc.pgm.util.xml.Node;

public interface FeatureValidation<T extends FeatureDefinition> {
  void validate(T definition, Node node) throws InvalidXMLException;
}
