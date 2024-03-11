package tc.oc.pgm.compass;

import java.lang.reflect.Method;
import java.util.Map;
import net.kyori.adventure.text.Component;
import org.jdom2.Element;
import tc.oc.pgm.api.filter.Filter;
import tc.oc.pgm.api.map.factory.MapFactory;
import tc.oc.pgm.compass.targets.PlayerCompassTarget;
import tc.oc.pgm.features.FeatureDefinitionContext;
import tc.oc.pgm.filters.parse.FilterParser;
import tc.oc.pgm.util.MethodParser;
import tc.oc.pgm.util.MethodParsers;
import tc.oc.pgm.util.xml.InvalidXMLException;
import tc.oc.pgm.util.xml.Node;
import tc.oc.pgm.util.xml.XMLUtils;

public class CompassParser {

  private final MapFactory factory;
  private final FeatureDefinitionContext features;
  private final FilterParser filters;
  private final Map<String, Method> methodParsers;

  public CompassParser(MapFactory factory) {
    this.factory = factory;
    this.features = factory.getFeatures();
    this.filters = factory.getFilters();
    this.methodParsers = MethodParsers.getMethodParsersForClass(getClass());
  }

  public boolean isTarget(Element el) {
    return getParserFor(el) != null;
  }

  protected Method getParserFor(Element el) {
    return methodParsers.get(el.getName().toLowerCase());
  }

  public CompassTarget parseCompassTarget(Element el) throws InvalidXMLException {
    Method parser = getParserFor(el);
    if (parser != null) {
      try {
        return (CompassTarget) parser.invoke(this, el);
      } catch (Exception e) {
        throw InvalidXMLException.coerce(e, new Node(el));
      }
    } else {
      throw new InvalidXMLException("Unknown compass tracker type: " + el.getName(), el);
    }
  }

  @MethodParser("player")
  public PlayerCompassTarget parsePlayerTarget(Element el) throws InvalidXMLException {
    Component name = XMLUtils.parseFormattedText(Node.fromChildOrAttr(el, "name"));
    Filter filter = filters.parseProperty(el, "filter");
    boolean showPlayerName = XMLUtils.parseBoolean(Node.fromAttr(el, "show-player"), false);

    return new PlayerCompassTarget(filter, name, showPlayerName);
  }
}
