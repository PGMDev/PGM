package tc.oc.pgm.proximity;

import com.google.common.collect.Sets;
import java.util.Set;
import java.util.logging.Logger;
import org.bukkit.ChatColor;
import org.jdom2.Attribute;
import org.jdom2.Document;
import org.jdom2.Element;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.filters.FilterModule;
import tc.oc.pgm.filters.FilterParser;
import tc.oc.pgm.filters.InverseFilter;
import tc.oc.pgm.map.MapModule;
import tc.oc.pgm.map.MapModuleContext;
import tc.oc.pgm.match.MatchModule;
import tc.oc.pgm.module.ModuleDescription;
import tc.oc.pgm.regions.RegionModule;
import tc.oc.pgm.teams.TeamModule;
import tc.oc.pgm.util.XMLUtils;
import tc.oc.xml.InvalidXMLException;

@ModuleDescription(
    name = "Proximity Alarm",
    depends = {TeamModule.class, RegionModule.class, FilterModule.class})
public class ProximityAlarmModule extends MapModule {
  private final Set<ProximityAlarmDefinition> definitions;

  public ProximityAlarmModule(Set<ProximityAlarmDefinition> definitions) {
    this.definitions = definitions;
  }

  @Override
  public MatchModule createMatchModule(Match match) {
    return new ProximityAlarmMatchModule(match, this.definitions);
  }

  public static MapModule parse(MapModuleContext context, Logger logger, Document doc)
      throws InvalidXMLException {
    Set<ProximityAlarmDefinition> definitions = Sets.newHashSet();

    for (Element elAlarm :
        XMLUtils.flattenElements(doc.getRootElement(), "proximity-alarms", "proximity-alarm")) {
      definitions.add(parseDefinition(context, elAlarm));
    }

    if (definitions.isEmpty()) {
      return null;
    } else {
      return new ProximityAlarmModule(definitions);
    }
  }

  public static ProximityAlarmDefinition parseDefinition(MapModuleContext context, Element elAlarm)
      throws InvalidXMLException {
    ProximityAlarmDefinition definition = new ProximityAlarmDefinition();

    FilterParser filterParser = context.getFilterParser();
    definition.detectFilter = filterParser.parseRequiredFilterProperty(elAlarm, "detect");
    definition.alertFilter =
        filterParser.parseFilterProperty(
            elAlarm, "notify", new InverseFilter(definition.detectFilter));

    definition.detectRegion =
        context.getRegionParser().parseRequiredRegionProperty(elAlarm, "region");
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
