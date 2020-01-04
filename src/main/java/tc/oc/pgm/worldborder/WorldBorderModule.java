package tc.oc.pgm.worldborder;

import com.google.common.collect.ImmutableList;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;
import org.jdom2.Document;
import org.jdom2.Element;
import org.joda.time.Duration;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.filters.Filter;
import tc.oc.pgm.filters.StaticFilter;
import tc.oc.pgm.filters.TimeFilter;
import tc.oc.pgm.map.MapModule;
import tc.oc.pgm.map.MapModuleContext;
import tc.oc.pgm.maptag.MapTag;
import tc.oc.pgm.match.MatchModule;
import tc.oc.pgm.module.ModuleDescription;
import tc.oc.pgm.util.XMLUtils;
import tc.oc.xml.InvalidXMLException;
import tc.oc.xml.Node;

@ModuleDescription(name = "World Border")
public class WorldBorderModule extends MapModule {

  private static final MapTag WORLDBORDER_TAG = MapTag.forName("worldborder");

  private final List<WorldBorder> borders;

  public WorldBorderModule(List<WorldBorder> borders) {
    this.borders = borders;
  }

  @Override
  @SuppressWarnings("unchecked")
  public void loadTags(Set tags) {
    tags.add(WORLDBORDER_TAG);
  }

  @Override
  public MatchModule createMatchModule(Match match) {
    return new WorldBorderMatchModule(match, borders);
  }

  public static WorldBorderModule parse(MapModuleContext context, Logger logger, Document doc)
      throws InvalidXMLException {
    List<WorldBorder> borders = new ArrayList<>();
    for (Element el :
        XMLUtils.flattenElements(doc.getRootElement(), "world-borders", "world-border")) {
      Filter filter = context.getFilterParser().parseFilterProperty(el, "when", StaticFilter.ALLOW);

      Duration after = XMLUtils.parseDuration(Node.fromAttr(el, "after"));
      if (after != null) {
        if (filter != StaticFilter.ALLOW) {
          throw new InvalidXMLException(
              "Cannot combine a filter and an explicit time for a world border", el);
        }
        filter = new TimeFilter(after);
      }

      WorldBorder border =
          new WorldBorder(
              filter,
              XMLUtils.parse2DVector(Node.fromRequiredAttr(el, "center")),
              XMLUtils.parseNumber(Node.fromRequiredAttr(el, "size"), Double.class),
              XMLUtils.parseDuration(Node.fromAttr(el, "duration"), Duration.ZERO),
              XMLUtils.parseNumber(Node.fromAttr(el, "damage"), Double.class, 0.2d),
              XMLUtils.parseNumber(Node.fromAttr(el, "buffer"), Double.class, 5d),
              XMLUtils.parseNumber(Node.fromAttr(el, "warning-distance"), Double.class, 5d),
              XMLUtils.parseDuration(
                  Node.fromAttr(el, "warning-time"), Duration.standardSeconds(15)));

      borders.add(border);
    }

    if (borders.isEmpty()) {
      return null;
    } else {
      return new WorldBorderModule(ImmutableList.copyOf(borders));
    }
  }
}
