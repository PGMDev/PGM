package tc.oc.pgm.api.feature;

import tc.oc.pgm.util.xml.InvalidXMLException;
import tc.oc.pgm.util.xml.Node;
import tc.oc.pgm.util.xml.Validator;

public interface FeatureValidation<T extends FeatureDefinition> extends Validator<T> {
  void validate(T definition, Node node) throws InvalidXMLException;
}
