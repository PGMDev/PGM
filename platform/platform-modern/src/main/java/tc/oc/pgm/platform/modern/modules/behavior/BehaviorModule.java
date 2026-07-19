package tc.oc.pgm.platform.modern.modules.behavior;

import com.google.common.collect.ImmutableMap;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import org.bukkit.util.Vector;
import org.jdom2.Document;
import org.jdom2.Element;
import tc.oc.pgm.action.Action;
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
import tc.oc.pgm.platform.modern.modules.behavior.pathing.PathingBehavior;
import tc.oc.pgm.platform.modern.modules.behavior.pathing.PathingGoalBehavior;
import tc.oc.pgm.platform.modern.modules.behavior.pathing.RelocationMethod;
import tc.oc.pgm.platform.modern.modules.behavior.pathing.RelocationType;
import tc.oc.pgm.platform.modern.modules.behavior.pathing.StuckBehavior;
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
        boolean avoidDanger = parser.parseBool(el, "avoid-danger").optional(false);

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

        PathingBehavior path = null;
        StuckBehavior stuck = null;
        Element pathEl = el.getChild("pathing");
        if (pathEl != null) {
          Node startNode = Node.fromAttr(pathEl, "start");
          Vector start = startNode != null ? XMLUtils.parseVector(startNode) : null;

          boolean loop = parser.parseBool(pathEl, "loop").optional(false);

          List<PathingGoalBehavior> goals = new ArrayList<>();
          for (Element goalEl : pathEl.getChildren("goal")) {
            Vector destination =
                XMLUtils.parseVector(XMLUtils.getRequiredAttribute(goalEl, "destination"));

            Duration idle = parser.duration(goalEl, "idle").orNull();
            Action<? super Match> completionAction =
                parser.action(Match.class, goalEl, "completion-action").orNull();
            Float goalRadius = parser.parseFloat(goalEl, "goal-radius").orNull();

            goals.add(new PathingGoalBehavior(destination, idle, completionAction, goalRadius));
          }

          if (goals.isEmpty()) {
            throw new InvalidXMLException(
                "Pathing requires at least one 'goal' sub-element to be defined", pathEl);
          }

          Element stuckEl = pathEl.getChild("if-stuck");
          if (stuckEl != null) {
            Duration after = parser.duration(stuckEl, "after").orNull();
            Action<? super Match> stuckAction =
                parser.action(Match.class, stuckEl, "stuck-action").orNull();

            RelocationType moveTo =
                parser.parseEnum(RelocationType.class, stuckEl, "move-to").orNull();
            RelocationMethod method =
                parser.parseEnum(RelocationMethod.class, stuckEl, "method").orNull();

            boolean giveUp = parser.parseBool(stuckEl, "give-up").optional(false);
            boolean despawn = parser.parseBool(stuckEl, "despawn").optional(false);
            if (despawn && (moveTo != null || method != null || giveUp)) {
              throw new InvalidXMLException(
                  "'despawn' cannot be combined with 'move-to', 'method', or 'give-up' attributes",
                  stuckEl);
            }

            stuck = new StuckBehavior(after, stuckAction, moveTo, method, giveUp, despawn);
          }

          path = new PathingBehavior(start, loop, goals, stuck);
        }

        BehaviorDefinition behaviorDefinition =
            new BehaviorDefinition(id, avoidDanger, combat, look, path);
        factory.getFeatures().addFeature(el, behaviorDefinition);
        behaviors.put(id, behaviorDefinition);
      }

      return behaviors.isEmpty() ? null : new BehaviorModule(ImmutableMap.copyOf(behaviors));
    }
  }
}
