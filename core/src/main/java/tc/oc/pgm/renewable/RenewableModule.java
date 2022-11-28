package tc.oc.pgm.renewable;

import com.google.common.collect.ImmutableList;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.logging.Logger;
import org.jdom2.Attribute;
import org.jdom2.Document;
import org.jdom2.Element;
import tc.oc.pgm.api.filter.Filter;
import tc.oc.pgm.api.map.MapModule;
import tc.oc.pgm.api.map.factory.MapFactory;
import tc.oc.pgm.api.map.factory.MapModuleFactory;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.match.MatchModule;
import tc.oc.pgm.filters.FilterModule;
import tc.oc.pgm.filters.matcher.StaticFilter;
import tc.oc.pgm.filters.operator.AllFilter;
import tc.oc.pgm.filters.operator.AnyFilter;
import tc.oc.pgm.filters.parse.FilterParser;
import tc.oc.pgm.regions.EverywhereRegion;
import tc.oc.pgm.regions.RegionModule;
import tc.oc.pgm.regions.RegionParser;
import tc.oc.pgm.snapshot.SnapshotMatchModule;
import tc.oc.pgm.util.xml.InvalidXMLException;
import tc.oc.pgm.util.xml.XMLUtils;

public class RenewableModule implements MapModule<RenewableMatchModule> {
  private static final double DEFAULT_AVOID_PLAYERS_RANGE = 2d;

  private final List<RenewableDefinition> renewableDefinitions = new ArrayList<>();

  @Override
  public Collection<Class<? extends MatchModule>> getHardDependencies() {
    return ImmutableList.of(SnapshotMatchModule.class);
  }

  @Override
  public RenewableMatchModule createMatchModule(Match match) {
    return new RenewableMatchModule(match, this.renewableDefinitions);
  }

  public static class Factory implements MapModuleFactory<RenewableModule> {
    @Override
    public Collection<Class<? extends MapModule<?>>> getWeakDependencies() {
      return ImmutableList.of(RegionModule.class, FilterModule.class);
    }

    @Override
    public RenewableModule parse(MapFactory factory, Logger logger, Document doc)
        throws InvalidXMLException {
      RenewableModule renewableModule = new RenewableModule();
      RegionParser regionParser = factory.getRegions();
      FilterParser filterParser = factory.getFilters();

      for (Element elRenewable :
          XMLUtils.flattenElements(doc.getRootElement(), "renewables", "renewable")) {
        RenewableDefinition renewableDefinition = new RenewableDefinition();
        renewableDefinition.region =
            regionParser.parseProperty(elRenewable, "region", EverywhereRegion.INSTANCE);

        renewableDefinition.renewableBlocks =
            parseFilter(filterParser, elRenewable, "renew", StaticFilter.ALLOW);
        renewableDefinition.replaceableBlocks =
            parseFilter(filterParser, elRenewable, "replace", StaticFilter.ALLOW);
        renewableDefinition.shuffleableBlocks =
            parseFilter(filterParser, elRenewable, "shuffle", StaticFilter.DENY);

        Attribute attrRate = elRenewable.getAttribute("rate");
        Attribute attrInterval = elRenewable.getAttribute("interval");
        if (attrRate != null) {
          if (attrInterval != null) {
            throw new InvalidXMLException(
                "Attributes 'rate' and 'interval' cannot be combined", elRenewable);
          } else {
            renewableDefinition.renewalsPerSecond = XMLUtils.parseNumber(attrRate, Float.class);
          }
        } else {
          if (attrInterval != null) {
            Duration interval = XMLUtils.parseDuration(attrInterval);
            renewableDefinition.renewalsPerSecond = 1000f / interval.toMillis();
            renewableDefinition.rateScaled = true;
          } else {
            renewableDefinition.renewalsPerSecond = 1f;
          }
        }

        renewableDefinition.growAdjacent =
            XMLUtils.parseBoolean(elRenewable.getAttribute("grow"), true);

        renewableDefinition.particles =
            XMLUtils.parseBoolean(elRenewable.getAttribute("particles"), true);

        renewableDefinition.sound = XMLUtils.parseBoolean(elRenewable.getAttribute("sound"), true);

        if (!XMLUtils.parseBoolean(elRenewable.getAttribute("avoid-entities"), true)) {
          // Legacy compatibility
          renewableDefinition.avoidPlayersRange = 0;
        } else {
          renewableDefinition.avoidPlayersRange =
              XMLUtils.parseNumber(
                  elRenewable.getAttribute("avoid-players"),
                  Double.class,
                  DEFAULT_AVOID_PLAYERS_RANGE);
        }

        renewableModule.renewableDefinitions.add(renewableDefinition);
      }

      return renewableModule.renewableDefinitions.isEmpty() ? null : renewableModule;
    }

    private static Filter parseFilter(FilterParser parser, Element el, String name, Filter def)
        throws InvalidXMLException {
      Filter property = parser.parseFilterProperty(el, name + "-filter");
      List<Filter> inline = new ArrayList<>();
      for (Element child : el.getChildren(name)) {
        inline.add(parser.parseMaterial(child));
      }
      if (property == null) {
        if (inline.isEmpty()) {
          return def;
        } else {
          return AnyFilter.of(inline);
        }
      } else {
        if (inline.isEmpty()) {
          return property;
        } else {
          return AllFilter.of(property, AnyFilter.of(inline));
        }
      }
    }
  }
}
