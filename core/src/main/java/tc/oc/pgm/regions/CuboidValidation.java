package tc.oc.pgm.regions;

import tc.oc.pgm.api.feature.FeatureValidation;
import tc.oc.pgm.api.region.RegionDefinition;
import tc.oc.pgm.util.xml.InvalidXMLException;
import tc.oc.pgm.util.xml.Node;

public class CuboidValidation implements FeatureValidation<RegionDefinition> {
  public static final CuboidValidation INSTANCE = new CuboidValidation();

  @Override
  public void validate(RegionDefinition definition, Node node) throws InvalidXMLException {
    if (!(definition instanceof CuboidRegion)) {
      throw new InvalidXMLException("region must be a cuboid", node);
    }
  }
}
