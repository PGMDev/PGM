package tc.oc.pgm.platform.modern.modules.behavior;

import com.google.common.collect.ImmutableMap;
import org.jdom2.Document;
import org.jdom2.Element;
import tc.oc.pgm.api.map.MapModule;
import tc.oc.pgm.api.map.factory.MapFactory;
import tc.oc.pgm.api.map.factory.MapModuleFactory;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.region.Region;
import tc.oc.pgm.platform.modern.modules.behavior.combat.CombatBehavior;
import tc.oc.pgm.platform.modern.modules.behavior.combat.HostilityType;
import tc.oc.pgm.util.xml.InvalidXMLException;
import tc.oc.pgm.util.xml.XMLUtils;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

public record BehaviorModule(Map<String, BehaviorDefinition> behaviorDefinitions)
    implements MapModule<BehaviorMatchModule> {

  @Override
  public BehaviorMatchModule createMatchModule(Match match) {
    return new BehaviorMatchModule(match, behaviorDefinitions);
  }

  public static class Factory implements MapModuleFactory<BehaviorModule> {
    @Override
    public BehaviorModule parse(MapFactory factory, Logger logger, Document doc)
        throws InvalidXMLException {
      Map<String, BehaviorDefinition> behaviors = new HashMap<>();
      var parser = factory.getParser();

      for (Element el : XMLUtils.flattenElements(doc.getRootElement(), "behaviors", "behavior")) {
        String id = parser.string(el, "id").required();

        Element combatEl = el.getChild("combat");
        CombatBehavior combat = null;
        if (combatEl != null) {
          HostilityType hostility = parser
              .parseEnum(HostilityType.class, combatEl, "hostility")
              .optional(HostilityType.PASSIVE);
          Region home = parser.region(combatEl, "home").orNull();
          Float radius = parser.parseFloat(combatEl, "home-radius").orNull();
          Duration duration = parser.duration(combatEl, "aggro-duration").orNull();
          combat = new CombatBehavior(hostility, home, radius, duration);
        }

        BehaviorDefinition behaviorDefinition = new BehaviorDefinition(id, combat);
        factory.getFeatures().addFeature(el, behaviorDefinition);
        behaviors.put(id, behaviorDefinition);
      }

      return behaviors.isEmpty() ? null : new BehaviorModule(ImmutableMap.copyOf(behaviors));
    }
  }
}
