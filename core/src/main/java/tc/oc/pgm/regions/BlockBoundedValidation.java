package tc.oc.pgm.regions;

import tc.oc.pgm.api.feature.FeatureValidation;
import tc.oc.pgm.api.region.RegionDefinition;
import tc.oc.pgm.util.xml.InvalidXMLException;
import tc.oc.pgm.util.xml.Node;

public class BlockBoundedValidation implements FeatureValidation<RegionDefinition> {
  public static final BlockBoundedValidation INSTANCE = new BlockBoundedValidation();

  @Override
  public void validate(RegionDefinition definition, Node node) throws InvalidXMLException {
    if (!definition.isBlockBounded()) {
      throw new InvalidXMLException("Cannot enumerate blocks in region", node);
    }
  }
}
