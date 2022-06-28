package tc.oc.pgm.regions;

import org.jdom2.Attribute;
import org.jdom2.Element;
import tc.oc.pgm.api.MethodParser;
import tc.oc.pgm.api.map.factory.MapFactory;
import tc.oc.pgm.api.region.Region;
import tc.oc.pgm.api.xml.InvalidXMLException;
import tc.oc.pgm.util.xml.XMLUtils;

/** For proto < 1.4 */
public class LegacyRegionParser extends RegionParserImpl {

  protected final RegionContext regionContext = new RegionContext();

  public LegacyRegionParser(MapFactory factory) {
    super(factory);
  }

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

  public Region parseReference(Attribute attr) throws InvalidXMLException {
    String name = attr.getValue();
    Region region = this.regionContext.get(name);
    if (region == null) {
      throw new InvalidXMLException("Unknown region '" + name + "'", attr);
    } else {
      return region;
    }
  }

  @MethodParser("region")
  public Region parseRegionTag(Element el) throws InvalidXMLException {
    return this.parseReference(XMLUtils.getRequiredAttribute(el, "name"));
  }
}
