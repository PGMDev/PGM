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
import tc.oc.pgm.api.filter.Filter;
import tc.oc.pgm.api.map.MapModule;
import tc.oc.pgm.api.map.factory.MapFactory;
import tc.oc.pgm.api.map.factory.MapModuleFactory;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.region.Region;
import tc.oc.pgm.platform.modern.modules.behavior.combat.CombatBehavior;
import tc.oc.pgm.platform.modern.modules.behavior.combat.CombatInstance;
import tc.oc.pgm.platform.modern.modules.behavior.combat.HostilityType;
import tc.oc.pgm.platform.modern.modules.behavior.evade.EvadeBehavior;
import tc.oc.pgm.platform.modern.modules.behavior.evade.EvadeInstance;
import tc.oc.pgm.platform.modern.modules.behavior.home.HomeBehavior;
import tc.oc.pgm.platform.modern.modules.behavior.looking.LookBehavior;
import tc.oc.pgm.platform.modern.modules.behavior.looking.RotationType;
import tc.oc.pgm.platform.modern.modules.behavior.pathing.PathingBehavior;
import tc.oc.pgm.platform.modern.modules.behavior.pathing.PathingGoalBehavior;
import tc.oc.pgm.platform.modern.modules.behavior.pathing.RelocationMethod;
import tc.oc.pgm.platform.modern.modules.behavior.pathing.RelocationType;
import tc.oc.pgm.platform.modern.modules.behavior.pathing.StuckBehavior;
import tc.oc.pgm.platform.modern.modules.behavior.tempt.TemptBehavior;
import tc.oc.pgm.platform.modern.modules.behavior.tempt.TemptInstance;
import tc.oc.pgm.util.inventory.ItemMatcher;
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
        boolean openDoors = parser.parseBool(el, "open-doors").optional(true);

        Element combatEl = el.getChild("combat");
        CombatBehavior combat = null;
        if (combatEl != null) {
          HostilityType hostility = parser
              .parseEnum(HostilityType.class, combatEl, "hostility")
              .optional(HostilityType.PASSIVE);
          Duration duration = parser.duration(combatEl, "aggro-duration").orNull();
          Float range = parser
              .parseFloat(combatEl, "attack-range")
              .optional(CombatInstance.DEFAULT_ATTACK_RANGE);
          Duration interval = parser
              .duration(combatEl, "attack-interval")
              .optional(CombatInstance.DEFAULT_ATTACK_INTERVAL);

          combat = new CombatBehavior(hostility, duration, range, interval);
        }

        Element homeEl = el.getChild("home");
        HomeBehavior home = null;
        if (homeEl != null) {
          Region region = parser.region(homeEl, "region").required();
          if (region == null) {
            throw new InvalidXMLException("'region' attribute is required", homeEl);
          }
          Boolean wander = parser.parseBool(homeEl, "wander").optional(false);
          Float strayDis = null;
          String stray = parser.string(homeEl, "stray-distance").orNull();
          if (stray != null && !stray.equalsIgnoreCase("outside")) {
            strayDis = XMLUtils.parseNumber(Node.fromAttr(homeEl, "stray-distance"), Float.class);
          }
          Duration returnAfter = parser.duration(homeEl, "return-after").orNull();
          if (returnAfter != null && strayDis == null) {
            throw new InvalidXMLException(
                "'return-after' requires 'stray-distance' to be defined", homeEl);
          }

          home = new HomeBehavior(region, wander, strayDis, returnAfter);
        }

        Element evadeEl = el.getChild("evade");
        EvadeBehavior evade = null;
        if (evadeEl != null) {
          Boolean panic = parser.parseBool(evadeEl, "panic").optional(false);
          Duration panicDuration = parser
              .duration(evadeEl, "panic-duration")
              .optional(EvadeInstance.DEFAULT_PANIC_DURATION);
          HostilityType hostility = combat != null ? combat.hostility() : HostilityType.PASSIVE;
          if ((hostility != HostilityType.PASSIVE) && (panic || panicDuration != null)) {
            throw new InvalidXMLException(
                "'panic' and 'panic-duration' attributes are not supported for non passive mannequins",
                evadeEl);
          }

          Float avoidRange = parser.parseFloat(evadeEl, "avoid-range").orNull();
          Filter avoidFilter = parser.filter(evadeEl, "avoid-filter").orNull();
          if ((hostility != HostilityType.PASSIVE) && (avoidRange != null || avoidFilter != null)) {
            throw new InvalidXMLException(
                "'avoid-range' and 'avoid-filter' attributes are only supported for PASSIVE mannequins",
                evadeEl);
          }

          evade = new EvadeBehavior(panic, panicDuration, avoidRange, avoidFilter);
        }

        TemptBehavior tempt = null;
        Element temptEl = el.getChild("tempt");
        if (temptEl != null) {
          var kits = factory.getKits();
          boolean follow = parser.parseBool(temptEl, "follow").optional(false);
          boolean hasHolding = temptEl.getChild("holding") != null;
          if (!follow && !hasHolding) {
            throw new InvalidXMLException(
                "Either a 'follow' attribute or 'holding' child must be defined", temptEl);
          }
          if (follow && hasHolding) {
            throw new InvalidXMLException("'follow' and 'holding' cannot both be defined", temptEl);
          }

          ItemMatcher holding = hasHolding ? kits.parseItemMatcher(temptEl, "holding") : null;
          Float range =
              parser.parseFloat(temptEl, "range").optional(TemptInstance.DEFAULT_TEMPT_RANGE);
          boolean leaveHome = parser.parseBool(temptEl, "leave-home").optional(false);

          tempt = new TemptBehavior(holding, follow, range, leaveHome);
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
          if (home != null) {
            throw new InvalidXMLException(
                "'home' cannot be combined with <pathing> use 'leash-distance' attribute on <pathing> instead",
                el);
          }

          Node startNode = Node.fromAttr(pathEl, "start");
          Vector start = startNode != null ? XMLUtils.parseVector(startNode) : null;

          boolean loop = parser.parseBool(pathEl, "loop").optional(false);
          Float leash = parser.parseFloat(pathEl, "leash-distance").orNull();

          List<PathingGoalBehavior> goals = new ArrayList<>();
          for (Element goalEl : pathEl.getChildren("goal")) {
            Vector destination =
                XMLUtils.parseVector(XMLUtils.getRequiredAttribute(goalEl, "destination"));

            Duration idle = parser.duration(goalEl, "idle").orNull();
            Action<? super Match> completionAction =
                parser.action(Match.class, goalEl, "completion-action").orNull();
            Float goalRadius = parser.parseFloat(goalEl, "goal-radius").orNull();
            if (goalRadius != null && goalRadius < 0.5f) {
              throw new InvalidXMLException("'goal-radius' must be at least 0.5", goalEl);
            }

            goals.add(new PathingGoalBehavior(destination, idle, completionAction, goalRadius));
          }

          if (goals.isEmpty()) {
            throw new InvalidXMLException(
                "Pathing requires at least one 'goal' sub-element to be defined", pathEl);
          }

          Element stuckEl = pathEl.getChild("if-stuck");
          if (stuckEl != null) {
            Duration after = parser.duration(stuckEl, "after").optional(Duration.ofSeconds(5));
            Action<? super Match> stuckAction =
                parser.action(Match.class, stuckEl, "stuck-action").orNull();

            RelocationType moveTo = null;
            Integer moveToIndex = null;
            String moveToString = parser.string(stuckEl, "move-to").attr().orNull();
            if (moveToString != null) {
              try {
                moveTo = RelocationType.valueOf(moveToString.toUpperCase());
              } catch (IllegalArgumentException error) {
                try {
                  moveToIndex = Integer.parseInt(moveToString);

                  if (moveToIndex < 0 || moveToIndex > goals.size()) {
                    throw new InvalidXMLException(
                        "move-to index must be between 0 and " + goals.size(), stuckEl);
                  }
                } catch (NumberFormatException exception) {
                  throw new InvalidXMLException(
                      "move-to must be 'start', 'end', 'previous', 'next', or a goal index number",
                      stuckEl);
                }
              }
            }

            RelocationMethod method =
                parser.parseEnum(RelocationMethod.class, stuckEl, "method").orNull();

            boolean giveUp = parser.parseBool(stuckEl, "give-up").optional(false);
            boolean despawn = parser.parseBool(stuckEl, "despawn").optional(false);
            if (despawn && (moveTo != null || method != null || giveUp)) {
              throw new InvalidXMLException(
                  "'despawn' cannot be combined with 'move-to', 'method', or 'give-up' attributes",
                  stuckEl);
            }

            stuck =
                new StuckBehavior(after, stuckAction, moveTo, moveToIndex, method, giveUp, despawn);

            if (stuck.moveTo() == RelocationType.NEXT
                && stuck.method() != RelocationMethod.TELEPORT) {
              throw new InvalidXMLException(
                  "move-to='next' can only be used when using method='teleport'", stuckEl);
            }
          }

          path = new PathingBehavior(start, loop, leash, goals, stuck);
        }

        BehaviorDefinition behaviorDefinition = new BehaviorDefinition(
            id, avoidDanger, openDoors, combat, home, evade, tempt, look, path);
        factory.getFeatures().addFeature(el, behaviorDefinition);
        behaviors.put(id, behaviorDefinition);
      }

      return behaviors.isEmpty() ? null : new BehaviorModule(ImmutableMap.copyOf(behaviors));
    }
  }
}
