package tc.oc.pgm.regions;

import tc.oc.pgm.api.feature.FeatureValidation;
import tc.oc.pgm.api.region.RegionDefinition;
import tc.oc.pgm.util.xml.InvalidXMLException;
import tc.oc.pgm.util.xml.Node;

public class StaticValidation implements FeatureValidation<RegionDefinition> {
  public static final StaticValidation INSTANCE = new StaticValidation();

  @Override
  public void validate(RegionDefinition definition, Node node) throws InvalidXMLException {
    if (!definition.isStatic()) {
      throw new InvalidXMLException("Cannot use non-static region here", node);
    }
  }
}
