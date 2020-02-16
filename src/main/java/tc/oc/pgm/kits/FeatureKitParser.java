package tc.oc.pgm.kits;

import org.jdom2.Element;
import tc.oc.pgm.api.map.factory.MapFactory;
import tc.oc.pgm.features.FeatureDefinitionContext;
import tc.oc.xml.InvalidXMLException;
import tc.oc.xml.Node;

public class FeatureKitParser extends KitParser {

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
    String id = FeatureDefinitionContext.parseId(el);

    if (id != null && maybeReference(el)) {
      kit = parseReference(new Node(el), id);
    } else {
      kit = parseDefinition(el);
      factory.getFeatures().addFeature(el, (KitDefinition) kit);
    }

    return kit;
  }
}
