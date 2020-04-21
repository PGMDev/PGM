package tc.oc.pgm.regions;

import tc.oc.pgm.api.feature.FeatureValidation;
import tc.oc.pgm.api.region.RegionDefinition;
import tc.oc.pgm.util.xml.InvalidXMLException;
import tc.oc.pgm.util.xml.Node;

public class RandomPointsValidation implements FeatureValidation<RegionDefinition> {
  public static final RandomPointsValidation INSTANCE = new RandomPointsValidation();

  @Override
  public void validate(RegionDefinition definition, Node node) throws InvalidXMLException {
    if (!definition.canGetRandom()) {
      throw new InvalidXMLException(
          "Cannot generate random points in region type " + definition.getClass().getSimpleName(),
          node);
    }
  }
}
