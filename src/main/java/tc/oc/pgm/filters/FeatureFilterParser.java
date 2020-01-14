package tc.oc.pgm.filters;

import org.jdom2.Element;
import tc.oc.pgm.api.map.factory.MapFactory;
import tc.oc.pgm.util.MethodParser;
import tc.oc.xml.InvalidXMLException;
import tc.oc.xml.Node;

public class FeatureFilterParser extends FilterParser {

  public FeatureFilterParser(MapFactory factory) {
    super(factory);
  }

  @Override
  public Filter parse(Element el) throws InvalidXMLException {
    Filter filter = this.parseDynamic(el);
    if (filter instanceof FilterDefinition) {
      factory.getFeatures().addFeature(el, (FilterDefinition) filter);
    }
    return filter;
  }

  @Override
  public Filter parseReference(Node node, String value) throws InvalidXMLException {
    return factory

        .getFeatures()
        .addReference(
            new XMLFilterReference(
                    factory.getFeatures(), node, value, FilterDefinition.class));
  }

  @MethodParser("filter")
  public Filter parseFilter(Element el) throws InvalidXMLException {
    return parseReference(Node.fromRequiredAttr(el, "id"));
  }

  @MethodParser("allow")
  public Filter parseAllow(Element el) throws InvalidXMLException {
    return new AllowFilter(parseChild(el));
  }

  @MethodParser("deny")
  public Filter parseDeny(Element el) throws InvalidXMLException {
    return new DenyFilter(parseChild(el));
  }

  // Override with a version that only accepts a single child filter
  @MethodParser("not")
  public Filter parseNot(Element el) throws InvalidXMLException {
    return new InverseFilter(parseChild(el));
  }
}
