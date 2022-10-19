package tc.oc.pgm.regions;

import org.jdom2.Element;
import tc.oc.pgm.api.feature.FeatureValidation;
import tc.oc.pgm.api.map.factory.MapFactory;
import tc.oc.pgm.api.region.Region;
import tc.oc.pgm.api.region.RegionDefinition;
import tc.oc.pgm.util.MethodParser;
import tc.oc.pgm.util.xml.InvalidXMLException;
import tc.oc.pgm.util.xml.Node;
import tc.oc.pgm.util.xml.XMLUtils;

/** For proto < 1.4 */
public class LegacyRegionParser extends RegionParser {

  protected final RegionContext regionContext = new RegionContext();

  public LegacyRegionParser(MapFactory factory) {
    super(factory);
  }

  @Override
  public Region parse(Element el) throws InvalidXMLException {
    Region region = this.parseDynamic(el);

    String name = el.getAttributeValue("name");
    if (name == null || el.getName().equalsIgnoreCase("region")) {
      this.regionContext.add(region);
    } else {
      if (this.regionContext.get(name) == null) {
        this.regionContext.add(name, region);
      } else {
        throw new InvalidXMLException("The region '" + name + "' already exists", el);
      }
    }

    return region;
  }

  @Override
  public Region parseReference(Node node, String id) throws InvalidXMLException {
    Region region = this.regionContext.get(id);
    if (region == null) {
      throw new InvalidXMLException("Unknown region '" + id + "'", node);
    } else {
      return region;
    }
  }

  @Override
  public void validate(Region region, FeatureValidation<RegionDefinition> validation, Node node)
      throws InvalidXMLException {
    validation.validate((RegionDefinition) region, node);
  }

  @MethodParser("region")
  public Region parseRegionTag(Element el) throws InvalidXMLException {
    return this.parseReference(XMLUtils.getRequiredAttribute(el, "name"));
  }
}
