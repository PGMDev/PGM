package tc.oc.pgm.regions;

import org.jdom2.Attribute;
import org.jdom2.Element;
import tc.oc.pgm.api.feature.FeatureValidation;
import tc.oc.pgm.api.map.factory.MapFactory;
import tc.oc.pgm.api.region.Region;
import tc.oc.pgm.api.region.RegionDefinition;
import tc.oc.pgm.util.MethodParser;
import tc.oc.pgm.util.xml.InvalidXMLException;
import tc.oc.pgm.util.xml.Node;
import tc.oc.pgm.util.xml.XMLUtils;

public class FeatureRegionParser extends RegionParser {

  public FeatureRegionParser(MapFactory factory) {
    super(factory);
  }

  @Override
  public Region parse(Element el) throws InvalidXMLException {
    Region region = this.parseDynamic(el);
    if (region instanceof RegionDefinition) {
      factory.getFeatures().addFeature(el, (RegionDefinition) region);
    }
    return region;
  }

  @Override
  public Region parseReference(Attribute attr) throws InvalidXMLException {
    return factory
        .getFeatures()
        .addReference(
            new XMLRegionReference(factory.getFeatures(), new Node(attr), RegionDefinition.class));
  }

  @MethodParser("region")
  public Region parseRegionTag(Element el) throws InvalidXMLException {
    return this.parseReference(XMLUtils.getRequiredAttribute(el, "id"));
  }

  @Override
  public void validate(Region region, FeatureValidation<RegionDefinition> validation, Node node)
      throws InvalidXMLException {
    if (region instanceof XMLRegionReference) {
      factory.getFeatures().validate((XMLRegionReference) region, validation);
    } else if (region instanceof TransformedRegion
        && ((TransformedRegion) region).region instanceof XMLRegionReference) {
      factory
          .getFeatures()
          .validate((XMLRegionReference) ((TransformedRegion) region).region, validation);
    } else {
      super.validate(region, validation, node);
    }
  }
}
