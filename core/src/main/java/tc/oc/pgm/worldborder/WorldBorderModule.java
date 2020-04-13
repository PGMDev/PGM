package tc.oc.pgm.worldborder;

import com.google.common.collect.ImmutableList;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.logging.Logger;
import org.jdom2.Document;
import org.jdom2.Element;
import tc.oc.pgm.api.filter.Filter;
import tc.oc.pgm.api.map.MapModule;
import tc.oc.pgm.api.map.MapTag;
import tc.oc.pgm.api.map.factory.MapFactory;
import tc.oc.pgm.api.map.factory.MapModuleFactory;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.match.MatchModule;
import tc.oc.pgm.filters.StaticFilter;
import tc.oc.pgm.filters.TimeFilter;
import tc.oc.util.xml.InvalidXMLException;
import tc.oc.util.xml.Node;
import tc.oc.util.xml.XMLUtils;

public class WorldBorderModule implements MapModule {
  private final Collection<MapTag> TAGS =
      ImmutableList.of(MapTag.create("border", "World Border", false, true));
  private final List<WorldBorder> borders;

  public WorldBorderModule(List<WorldBorder> borders) {
    this.borders = borders;
  }

  @Override
  public Collection<MapTag> getTags() {
    return TAGS;
  }

  @Override
  public MatchModule createMatchModule(Match match) {
    return new WorldBorderMatchModule(match, borders);
  }

  public static class Factory implements MapModuleFactory<WorldBorderModule> {
    @Override
    public WorldBorderModule parse(MapFactory factory, Logger logger, Document doc)
        throws InvalidXMLException {
      List<WorldBorder> borders = new ArrayList<>();
      for (Element el :
          XMLUtils.flattenElements(doc.getRootElement(), "world-borders", "world-border")) {
        Filter filter = factory.getFilters().parseFilterProperty(el, "when", StaticFilter.ALLOW);

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
                XMLUtils.parseDuration(Node.fromAttr(el, "warning-time"), Duration.ofSeconds(15)));

        borders.add(border);
      }

      if (borders.isEmpty()) {
        return null;
      } else {
        return new WorldBorderModule(ImmutableList.copyOf(borders));
      }
    }
  }
}
