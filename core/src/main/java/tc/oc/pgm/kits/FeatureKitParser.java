package tc.oc.pgm.kits;

import org.jdom2.Element;
import tc.oc.pgm.api.kits.Kit;
import tc.oc.pgm.api.kits.KitDefinition;
import tc.oc.pgm.api.map.factory.MapFactory;
import tc.oc.pgm.api.xml.InvalidXMLException;
import tc.oc.pgm.api.xml.Node;

public class FeatureKitParser extends KitParserImpl {

  public FeatureKitParser(MapFactory factory) {
    super(factory);
  }

  @Override
  public Kit parseReference(Node node, String name) throws InvalidXMLException {
    return factory
        .getFeatures()
        .addReference(new XMLKitReference(factory.getFeatures(), node, name, KitDefinition.class));
  }

  @Override
  public Kit parse(Element el) throws InvalidXMLException {
    Kit kit;
    String id = el.getAttributeValue("id");

    if (id != null && maybeReference(el)) {
      kit = parseReference(new Node(el), id);
    } else {
      kit = parseDefinition(el);
      factory.getFeatures().addFeature(el, (KitDefinition) kit);
    }

    return kit;
  }
}
