package tc.oc.pgm.regions;

import tc.oc.pgm.features.FeatureValidation;
import tc.oc.xml.InvalidXMLException;
import tc.oc.xml.Node;

public class CuboidValidation implements FeatureValidation<RegionDefinition> {
  public static final CuboidValidation INSTANCE = new CuboidValidation();

  @Override
  public void validate(RegionDefinition definition, Node node) throws InvalidXMLException {
    if (!(definition instanceof CuboidRegion)) {
      throw new InvalidXMLException("region must be a cuboid", node);
    }
  }
}
