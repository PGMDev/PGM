package tc.oc.pgm.compass;

import java.lang.reflect.Method;
import java.util.Map;
import net.kyori.adventure.text.Component;
import org.jdom2.Element;
import tc.oc.pgm.api.filter.Filter;
import tc.oc.pgm.api.map.factory.MapFactory;
import tc.oc.pgm.compass.targets.FlagCompassTarget;
import tc.oc.pgm.compass.targets.PlayerCompassTarget;
import tc.oc.pgm.features.FeatureDefinitionContext;
import tc.oc.pgm.filters.matcher.StaticFilter;
import tc.oc.pgm.filters.parse.FilterParser;
import tc.oc.pgm.flag.FlagDefinition;
import tc.oc.pgm.util.MethodParser;
import tc.oc.pgm.util.MethodParsers;
import tc.oc.pgm.util.xml.InvalidXMLException;
import tc.oc.pgm.util.xml.Node;
import tc.oc.pgm.util.xml.XMLUtils;

public class CompassParser {

  private final FeatureDefinitionContext features;
  private final FilterParser filters;
  private final Map<String, Method> methodParsers;

  public CompassParser(MapFactory factory) {
    this.features = factory.getFeatures();
    this.filters = factory.getFilters();
    this.methodParsers = MethodParsers.getMethodParsersForClass(getClass());
  }

  protected Method getParserFor(Element el) {
    return methodParsers.get(el.getName().toLowerCase());
  }

  public CompassTarget<?> parseCompassTarget(Element el) throws InvalidXMLException {
    Method parser = getParserFor(el);
    if (parser != null) {
      try {
        return (CompassTarget<?>) parser.invoke(this, el);
      } catch (Exception e) {
        throw InvalidXMLException.coerce(e, new Node(el));
      }
    } else {
      throw new InvalidXMLException("Unknown compass tracker type: " + el.getName(), el);
    }
  }

  @MethodParser("player")
  public PlayerCompassTarget parsePlayerTarget(Element el) throws InvalidXMLException {
    Filter holderFilter = filters.parseProperty(el, "holder-filter", StaticFilter.ALLOW);
    Filter targetFilter = filters.parseRequiredProperty(el, "filter");
    Component name = XMLUtils.parseFormattedText(Node.fromChildOrAttr(el, "name"));
    boolean showPlayerName = XMLUtils.parseBoolean(Node.fromAttr(el, "show-player"), false);

    return new PlayerCompassTarget(holderFilter, targetFilter, name, showPlayerName);
  }

  @MethodParser("flag")
  public FlagCompassTarget parseFlagTarget(Element el) throws InvalidXMLException {
    Filter holderFilter = filters.parseProperty(el, "holder-filter", StaticFilter.ALLOW);
    FlagDefinition flag = features.resolve(new Node(el), FlagDefinition.class);
    Component name = XMLUtils.parseFormattedText(Node.fromChildOrAttr(el, "name"));

    return new FlagCompassTarget(holderFilter, flag, name);
  }
}
