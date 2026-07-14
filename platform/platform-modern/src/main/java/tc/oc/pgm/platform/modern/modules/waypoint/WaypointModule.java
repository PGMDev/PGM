package tc.oc.pgm.platform.modern.modules.waypoint;

import com.google.common.collect.ImmutableMap;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.waypoints.WaypointStyleAsset;
import net.minecraft.world.waypoints.WaypointStyleAssets;
import org.bukkit.Color;
import org.jdom2.Document;
import org.jdom2.Element;
import tc.oc.pgm.api.map.MapModule;
import tc.oc.pgm.api.map.factory.MapFactory;
import tc.oc.pgm.api.map.factory.MapModuleFactory;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.util.xml.InvalidXMLException;
import tc.oc.pgm.util.xml.Node;
import tc.oc.pgm.util.xml.XMLUtils;

public record WaypointModule(Map<String, WaypointDefinition> waypointDefinitions)
    implements MapModule<WaypointMatchModule> {

  @Override
  public WaypointMatchModule createMatchModule(Match match) {
    return new WaypointMatchModule(match, waypointDefinitions);
  }

  public static class Factory implements MapModuleFactory<WaypointModule> {
    @Override
    public WaypointModule parse(MapFactory factory, Logger logger, Document doc)
        throws InvalidXMLException {
      Map<String, WaypointDefinition> waypoints = new HashMap<>();
      var parser = factory.getParser();

      for (Element el : XMLUtils.flattenElements(doc.getRootElement(), "waypoints", "waypoint")) {
        String id = parser.string(el, "id").required();
        String styleName = parser.string(el, "style").attr().optional("default");
        ResourceKey<WaypointStyleAsset> style = WaypointStyleAssets.createId(styleName);
        Color color = XMLUtils.parseHexColor(Node.fromRequiredAttr(el, "color"));

//        ADD TEAMCOLOR SUPPORT. ONLY WORKS FOR PLAYERS, COLOR AND TEAMCOLOR CANNOT BOTH BE DEFINED.
//        IF TEAM-COLOR="TRUE" AND APPLIED TO MANNEQUIN THE COLOR DEFAULTS TO WHITE

        Float transmitRange = parser.parseFloat(el, "transmit-range").attr().orNull();

        WaypointDefinition waypointDefinition =
            new WaypointDefinition(id, style, color, transmitRange);

        factory.getFeatures().addFeature(el, waypointDefinition);
        waypoints.put(id, waypointDefinition);
      }

      return waypoints.isEmpty() ? null : new WaypointModule(ImmutableMap.copyOf(waypoints));
    }
  }
}
