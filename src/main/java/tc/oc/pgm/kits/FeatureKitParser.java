package tc.oc.pgm.kits;

import org.jdom2.Element;
import tc.oc.pgm.api.map.MapContext;
import tc.oc.pgm.features.FeatureDefinitionContext;
import tc.oc.xml.InvalidXMLException;
import tc.oc.xml.Node;

public class FeatureKitParser extends KitParser {

  public FeatureKitParser(MapContext context) {
    super(context);
  }

  @Override
  public Kit parseReference(Node node, String name) throws InvalidXMLException {
    return context
        .legacy()
        .getFeatures()
        .addReference(
            new XMLKitReference(context.legacy().getFeatures(), node, name, KitDefinition.class));
  }

  @Override
  public Kit parse(Element el) throws InvalidXMLException {
    Kit kit;
    String id = FeatureDefinitionContext.parseId(el);

    if (id != null && maybeReference(el)) {
      kit = parseReference(new Node(el), id);
    } else {
      kit = parseDefinition(el);
      context.legacy().getFeatures().addFeature(el, (KitDefinition) kit);
    }

    return kit;
  }
}
