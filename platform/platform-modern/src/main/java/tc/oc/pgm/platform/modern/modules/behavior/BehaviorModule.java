package tc.oc.pgm.platform.modern.modules.behavior;

import com.google.common.collect.ImmutableMap;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;
import org.bukkit.util.Vector;
import org.jdom2.Document;
import org.jdom2.Element;
import tc.oc.pgm.api.map.MapModule;
import tc.oc.pgm.api.map.factory.MapFactory;
import tc.oc.pgm.api.map.factory.MapModuleFactory;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.region.Region;
import tc.oc.pgm.platform.modern.modules.behavior.combat.CombatBehavior;
import tc.oc.pgm.platform.modern.modules.behavior.combat.CombatInstance;
import tc.oc.pgm.platform.modern.modules.behavior.combat.HostilityType;
import tc.oc.pgm.platform.modern.modules.behavior.looking.LookBehavior;
import tc.oc.pgm.platform.modern.modules.behavior.looking.RotationType;
import tc.oc.pgm.util.xml.InvalidXMLException;
import tc.oc.pgm.util.xml.Node;
import tc.oc.pgm.util.xml.XMLUtils;

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
        boolean avoidDanger = parser.parseBool(el, "avoid-danger").optional(true);

        Element combatEl = el.getChild("combat");
        CombatBehavior combat = null;
        if (combatEl != null) {
          HostilityType hostility = parser
              .parseEnum(HostilityType.class, combatEl, "hostility")
              .optional(HostilityType.PASSIVE);
          Region home = parser.region(combatEl, "home").orNull();
          Float radius = parser.parseFloat(combatEl, "home-radius").orNull();
          Duration duration = parser.duration(combatEl, "aggro-duration").orNull();
          Float range = parser
              .parseFloat(combatEl, "attack-range")
              .optional(CombatInstance.DEFAULT_ATTACK_RANGE);
          Duration interval = parser
              .duration(combatEl, "attack-interval")
              .optional(CombatInstance.DEFAULT_ATTACK_INTERVAL);

          Boolean returnHome = parser.parseBool(combatEl, "return-home").optional(false);
          Float strayDis = null;
          String stray = parser.string(combatEl, "stray-distance").orNull();
          if (stray != null && !stray.equalsIgnoreCase("outside")) {
            strayDis = XMLUtils.parseNumber(Node.fromAttr(combatEl, "stray-distance"), Float.class);
          }
          Duration returnAfter = parser.duration(combatEl, "return-after").orNull();

          Boolean wander = parser.parseBool(combatEl, "wander").optional(false);
          Boolean panic = parser.parseBool(combatEl, "panic").optional(false);
          Duration panicDuration = parser.duration(combatEl, "panic-duration").orNull();
          if ((hostility != HostilityType.PASSIVE) && (panic || panicDuration != null)) {
            throw new InvalidXMLException(
                "'panic' and 'panic-duration' attributes are not supported for non passive mannequins",
                combatEl);
          }

          combat = new CombatBehavior(
              hostility,
              home,
              radius,
              duration,
              range,
              interval,
              returnHome,
              strayDis,
              returnAfter,
              wander,
              panic,
              panicDuration);
        }

        Element lookEl = el.getChild("look-at");
        LookBehavior look = null;
        if (lookEl != null) {
          String target = parser.string(lookEl, "target").orNull();
          boolean trackPlayer = target == null || target.equalsIgnoreCase("player");
          Vector trackPoint =
              trackPlayer ? null : XMLUtils.parseVector(Node.fromAttr(lookEl, "target"));

          RotationType rotation =
              parser.parseEnum(RotationType.class, lookEl, "rotation").optional(RotationType.BODY);
          Float range = parser.parseFloat(lookEl, "visibility-range").optional(6f);
          Float restYaw = parser.parseFloat(lookEl, "rest-yaw").orNull();
          Float restPitch = parser.parseFloat(lookEl, "rest-pitch").orNull();

          look = new LookBehavior(trackPlayer, trackPoint, rotation, range, restYaw, restPitch);
        }

        BehaviorDefinition behaviorDefinition =
            new BehaviorDefinition(id, avoidDanger, combat, look);
        factory.getFeatures().addFeature(el, behaviorDefinition);
        behaviors.put(id, behaviorDefinition);
      }

      return behaviors.isEmpty() ? null : new BehaviorModule(ImmutableMap.copyOf(behaviors));
    }
  }
}
