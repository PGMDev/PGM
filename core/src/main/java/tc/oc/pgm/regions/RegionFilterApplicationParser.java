package tc.oc.pgm.regions;

import static net.kyori.adventure.text.Component.translatable;

import com.google.common.base.Splitter;
import com.google.common.collect.Lists;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import net.kyori.adventure.text.Component;
import org.bukkit.util.Vector;
import org.jdom2.Attribute;
import org.jdom2.Element;
import tc.oc.pgm.api.filter.Filter;
import tc.oc.pgm.api.map.MapProtos;
import tc.oc.pgm.api.map.factory.MapFactory;
import tc.oc.pgm.api.region.Region;
import tc.oc.pgm.filters.matcher.StaticFilter;
import tc.oc.pgm.filters.matcher.party.TeamFilter;
import tc.oc.pgm.filters.operator.DenyFilter;
import tc.oc.pgm.filters.operator.FilterNode;
import tc.oc.pgm.filters.parse.FilterParser;
import tc.oc.pgm.kits.Kit;
import tc.oc.pgm.teams.Teams;
import tc.oc.pgm.util.Version;
import tc.oc.pgm.util.xml.InvalidXMLException;
import tc.oc.pgm.util.xml.Node;
import tc.oc.pgm.util.xml.XMLUtils;

public class RegionFilterApplicationParser {
  private final MapFactory factory;
  private final FilterParser filterParser;
  private final RegionParser regionParser;
  private final RFAContext rfaContext;
  private final Version proto;

  public RegionFilterApplicationParser(MapFactory factory, RFAContext rfaContext) {
    this.factory = factory;
    this.rfaContext = rfaContext;

    this.filterParser = factory.getFilters();
    this.regionParser = factory.getRegions();

    this.proto = factory.getProto();
  }

  private boolean useId() {
    return proto.isNoOlderThan(MapProtos.FILTER_FEATURES);
  }

  private Region parseRegion(Element el) throws InvalidXMLException {
    Region region;
    if (useId()) {
      region = regionParser.parseRegionProperty(el, "region");
      if (region == null) region = EverywhereRegion.INSTANCE;
    } else {
      region = regionParser.parseChildren(el);
    }
    return region;
  }

  private void add(Element el, RegionFilterApplication rfa) throws InvalidXMLException {
    factory.getFeatures().addFeature(el, rfa);
    rfaContext.add(rfa);
  }

  private void prepend(Element el, RegionFilterApplication rfa) throws InvalidXMLException {
    factory.getFeatures().addFeature(el, rfa);
    rfaContext.prepend(rfa);
  }

  public void parseLane(Element el) throws InvalidXMLException {
    final Filter filter =
        new DenyFilter(
            new TeamFilter(
                Teams.getTeamRef(new Node(XMLUtils.getRequiredAttribute(el, "team")), factory)));
    final Region region = parseRegion(el);
    final Component message = translatable("match.laneExit");

    prepend(el, new RegionFilterApplication(RFAScope.PLAYER_ENTER, region, filter, message, false));
    prepend(
        el,
        new RegionFilterApplication(
            RFAScope.BLOCK_PLACE, new NegativeRegion(region), filter, message, false));
  }

  public void parseMaxBuildHeight(Element el) throws InvalidXMLException {
    final Region region =
        new HalfspaceRegion(
            new Vector(0, XMLUtils.parseNumber(el, Integer.class), 0), new Vector(0, 1, 0));
    final Component message = translatable("match.maxBuildHeight");

    for (RFAScope scope : Lists.newArrayList(RFAScope.BLOCK_PLACE)) {
      prepend(el, new RegionFilterApplication(scope, region, StaticFilter.DENY, message, false));
    }
  }

  public void parsePlayable(Element el) throws InvalidXMLException {
    final Region region = new NegativeRegion(parseRegion(el));
    final Component message = translatable("match.outOfBounds");

    for (RFAScope scope :
        Lists.newArrayList(RFAScope.BLOCK_PLACE, RFAScope.BLOCK_BREAK, RFAScope.PLAYER_ENTER)) {
      prepend(el, new RegionFilterApplication(scope, region, StaticFilter.DENY, message, false));
    }
  }

  public void parse(Element el) throws InvalidXMLException {
    Region region = parseRegion(el);
    Component message = XMLUtils.parseFormattedText(el, "message");

    boolean earlyWarning = XMLUtils.parseBoolean(el.getAttribute("early-warning"), false);

    Filter effectFilter = filterParser.parseFilterProperty(el, "filter");

    Kit kit = factory.getKits().parseKitProperty(el, "kit");
    if (kit != null) {
      add(el, new RegionFilterApplication(RFAScope.EFFECT, region, effectFilter, kit, false));
    }

    kit = factory.getKits().parseKitProperty(el, "lend-kit");
    if (kit != null) {
      add(el, new RegionFilterApplication(RFAScope.EFFECT, region, effectFilter, kit, true));
    }

    Attribute attrVelocity = el.getAttribute("velocity");
    if (attrVelocity != null) {
      // Legacy support
      String velocityText = attrVelocity.getValue();
      if (velocityText.startsWith("@")) velocityText = velocityText.substring(1);
      Vector velocity = XMLUtils.parseVector(attrVelocity, velocityText);
      add(el, new RegionFilterApplication(RFAScope.EFFECT, region, effectFilter, velocity));
    }

    for (String tag : RFAScope.byTag.keySet()) {
      Filter filter;
      if (useId()) {
        filter = filterParser.parseFilterProperty(el, tag);
      } else {
        // Legacy syntax allows a list of filter names in the attribute
        Node node = Node.fromAttr(el, tag);
        if (node == null) {
          filter = null;
        } else {
          List<Filter> filters = new ArrayList<>();
          for (String name : Splitter.on(" ").split(node.getValue())) {
            filters.add(filterParser.parseReference(node, name));
          }
          switch (filters.size()) {
            case 0:
              filter = null;
              break;
            case 1:
              filter = filters.get(0);
              break;
            default:
              filter =
                  new FilterNode(
                      filters, Collections.<Filter>emptyList(), Collections.<Filter>emptyList());
          }
        }
      }

      if (filter != null) {
        for (RFAScope scope : RFAScope.byTag.get(tag)) {
          add(el, new RegionFilterApplication(scope, region, filter, message, earlyWarning));
        }
      }
    }
  }
}
