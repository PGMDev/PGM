package tc.oc.pgm.regions;

import tc.oc.pgm.features.FeatureValidation;
import tc.oc.xml.InvalidXMLException;
import tc.oc.xml.Node;

public class BlockBoundedValidation implements FeatureValidation<RegionDefinition> {
  public static final BlockBoundedValidation INSTANCE = new BlockBoundedValidation();

  @Override
  public void validate(RegionDefinition definition, Node node) throws InvalidXMLException {
    if (!definition.isBlockBounded()) {
      throw new InvalidXMLException("Cannot enumerate blocks in region", node);
    }
  }
}
