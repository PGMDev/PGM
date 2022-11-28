package tc.oc.pgm.proximity;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Sets;
import java.util.Collection;
import java.util.Set;
import java.util.logging.Logger;
import org.bukkit.ChatColor;
import org.jdom2.Attribute;
import org.jdom2.Document;
import org.jdom2.Element;
import tc.oc.pgm.api.map.MapModule;
import tc.oc.pgm.api.map.factory.MapFactory;
import tc.oc.pgm.api.map.factory.MapModuleFactory;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.filters.FilterModule;
import tc.oc.pgm.filters.operator.InverseFilter;
import tc.oc.pgm.filters.parse.FilterParser;
import tc.oc.pgm.regions.RegionModule;
import tc.oc.pgm.teams.TeamModule;
import tc.oc.pgm.util.xml.InvalidXMLException;
import tc.oc.pgm.util.xml.XMLUtils;

public class ProximityAlarmModule implements MapModule<ProximityAlarmMatchModule> {
  private final Set<ProximityAlarmDefinition> definitions;

  public ProximityAlarmModule(Set<ProximityAlarmDefinition> definitions) {
    this.definitions = definitions;
  }

  @Override
  public ProximityAlarmMatchModule createMatchModule(Match match) {
    return new ProximityAlarmMatchModule(match, this.definitions);
  }

  public static class Factory implements MapModuleFactory<ProximityAlarmModule> {
    @Override
    public Collection<Class<? extends MapModule<?>>> getSoftDependencies() {
      return ImmutableList.of(TeamModule.class, RegionModule.class, FilterModule.class);
    }

    @Override
    public ProximityAlarmModule parse(MapFactory factory, Logger logger, Document doc)
        throws InvalidXMLException {
      Set<ProximityAlarmDefinition> definitions = Sets.newHashSet();

      for (Element elAlarm :
          XMLUtils.flattenElements(doc.getRootElement(), "proximity-alarms", "proximity-alarm")) {
        definitions.add(parseDefinition(factory, elAlarm));
      }

      if (definitions.isEmpty()) {
        return null;
      } else {
        return new ProximityAlarmModule(definitions);
      }
    }

    private static ProximityAlarmDefinition parseDefinition(MapFactory factory, Element elAlarm)
        throws InvalidXMLException {
      ProximityAlarmDefinition definition = new ProximityAlarmDefinition();

      FilterParser filterParser = factory.getFilters();
      definition.detectFilter = filterParser.parseRequiredFilterProperty(elAlarm, "detect");
      definition.alertFilter =
          filterParser.parseFilterProperty(
              elAlarm, "notify", new InverseFilter(definition.detectFilter));

      definition.detectRegion = factory.getRegions().parseRequiredRegionProperty(elAlarm, "region");
      definition.alertMessage = elAlarm.getAttributeValue("message"); // null = no message

      if (definition.alertMessage != null) {
        definition.alertMessage =
            ChatColor.translateAlternateColorCodes('`', definition.alertMessage);
      }
      Attribute attrFlareRadius = elAlarm.getAttribute("flare-radius");
      definition.flares = attrFlareRadius != null;
      if (definition.flares) {
        definition.flareRadius = XMLUtils.parseNumber(attrFlareRadius, Double.class);
      }

      return definition;
    }
  }
}
