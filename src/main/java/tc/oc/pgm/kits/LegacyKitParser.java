package tc.oc.pgm.kits;

import org.jdom2.Attribute;
import org.jdom2.Element;
import tc.oc.pgm.api.map.factory.MapFactory;
import tc.oc.xml.InvalidXMLException;
import tc.oc.xml.Node;

public class LegacyKitParser extends KitParser {

  private final KitContext kitContext = new KitContext();

  public LegacyKitParser(MapFactory factory) {
    super(factory);
  }

  @Override
  public Kit parseReference(Node node, String name) throws InvalidXMLException {
    Kit kit = this.kitContext.get(name);
    if (kit == null) {
      throw new InvalidXMLException("Unknown kit '" + name + "'", node);
    }
    return kit;
  }

  @Override
  public Kit parse(Element el) throws InvalidXMLException {
    Kit kit;
    Attribute attrName = el.getAttribute("name");

    if (attrName != null && maybeReference(el)) {
      kit = parseReference(new Node(el), attrName.getValue());
    } else {
      kit = parseDefinition(el);

      if (attrName != null) {
        try {
          kitContext.add(attrName.getValue(), kit);
        } catch (IllegalArgumentException e) {
          // Probably a duplicate name
          throw new InvalidXMLException(e.getMessage(), el);
        }
      } else {
        kitContext.add(kit);
      }
    }

    return kit;
  }
}
