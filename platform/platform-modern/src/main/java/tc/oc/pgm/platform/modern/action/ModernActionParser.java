package tc.oc.pgm.platform.modern.action;

import java.util.Optional;
import java.util.UUID;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import org.jdom2.Element;
import tc.oc.pgm.action.ActionParser;
import tc.oc.pgm.api.feature.FeatureReference;
import tc.oc.pgm.api.map.factory.MapFactory;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.filters.Filterable;
import tc.oc.pgm.platform.modern.action.actions.ModifyMannequinAction;
import tc.oc.pgm.platform.modern.action.actions.SpawnMannequinAction;
import tc.oc.pgm.platform.modern.action.actions.WaypointAction;
import tc.oc.pgm.platform.modern.modules.mannequin.MannequinDefinition;
import tc.oc.pgm.platform.modern.modules.mannequin.MannequinPose;
import tc.oc.pgm.platform.modern.modules.mannequin.SkinPart;
import tc.oc.pgm.platform.modern.modules.waypoint.WaypointDefinition;
import tc.oc.pgm.util.MethodParser;
import tc.oc.pgm.util.math.Formula;
import tc.oc.pgm.util.skin.Skin;
import tc.oc.pgm.util.xml.InvalidXMLException;
import tc.oc.pgm.util.xml.Node;
import tc.oc.pgm.util.xml.XMLUtils;

public class ModernActionParser extends ActionParser {

  public ModernActionParser(MapFactory factory) {
    super(factory);
  }

  @MethodParser("waypoint")
  public WaypointAction parseWaypoint(Element el, Class<?> scope) throws InvalidXMLException {
    var waypoint = getParser().reference(WaypointDefinition.class, el, "set").orNull();
    Boolean remove = getParser().parseBool(el, "remove").attr().orNull();
    if (!MatchPlayer.class.isAssignableFrom(scope)) {
      throw new InvalidXMLException(
          "Waypoint only supports player-scope", el); // Fix this wording later
    }
    if (waypoint != null && remove != null && remove) {
      throw new InvalidXMLException(
          "Cannot define both 'set' and 'remove' attributes", el); // Fix this wording later
    }
    if (waypoint == null && (remove == null || !remove)) {
      throw new InvalidXMLException(
          "Missing either 'set' or 'remove' attributes", el); // Fix this wording later
    }
    return new WaypointAction(waypoint, remove);
  }

  @MethodParser("spawn-mannequin")
  public <B extends Filterable<?>> SpawnMannequinAction<B> parseSpawnMannequin(
      Element el, Class<B> scope) throws InvalidXMLException {
    scope = parseScope(el, scope);
    var mannequin =
        getParser().reference(MannequinDefinition.class, el, "mannequin").required();
    var xformula = getParser().formula(scope, el, "x").required();
    var yformula = getParser().formula(scope, el, "y").required();
    var zformula = getParser().formula(scope, el, "z").required();
    var yawFormula = getParser().formula(scope, el, "yaw").optional();
    var pitchFormula = getParser().formula(scope, el, "pitch").optional();

    return new SpawnMannequinAction<>(
        scope, mannequin, xformula, yformula, zformula, yawFormula, pitchFormula);
  }

  @MethodParser("modify-mannequin")
  public <B extends Filterable<?>> ModifyMannequinAction<B> parseModifyMannequin(
      Element el, Class<B> scope) throws InvalidXMLException {
    scope = parseScope(el, scope);
    var mannequin =
        getParser().reference(MannequinDefinition.class, el, "mannequin").required();
    Component name = XMLUtils.parseFormattedText(Node.fromChildOrAttr(el, "name"));
    Component description = XMLUtils.parseFormattedText(Node.fromChildOrAttr(el, "description"));
    Boolean hideDescription =
        getParser().parseBool(el, "hide-description").attr().orNull();
    Boolean hideTitles = getParser().parseBool(el, "hide-titles").attr().orNull();
    Float health = getParser().parseFloat(el, "health").attr().orNull();
    Boolean silent = getParser().parseBool(el, "silent").attr().orNull();
    Boolean invulnerable = getParser().parseBool(el, "invulnerable").attr().orNull();
    TextColor glowing = getParser().textColor(el, "glowing").attr().orNull();
    Boolean immovable = getParser().parseBool(el, "immovable").attr().orNull();
    Boolean gravity = getParser().parseBool(el, "gravity").attr().orNull();
    Boolean physics = getParser().parseBool(el, "physics").attr().orNull();
    Boolean onFire = getParser().parseBool(el, "on-fire").attr().orNull();

    UUID uuid = null;
    Skin skin = null;
    SkinPart.SkinLayers layers = null;
    MannequinPose pose = null;
    Element profileEl = el.getChild("profile");
    if (profileEl != null) {
      Node uuidNode = Node.fromAttr(profileEl, "uuid");
      Node skinNode = Node.fromChildOrAttr(profileEl, "skin");
      if (uuidNode != null && skinNode != null) {
        uuid = XMLUtils.parseUuid(Node.fromRequiredAttr(profileEl, "uuid"));
        skin = XMLUtils.parseUnsignedSkin(Node.fromRequiredChildOrAttr(profileEl, "skin"));
      } else if (uuidNode != null || skinNode != null) {
        throw new InvalidXMLException(
            "Skin changes require both 'uuid' and 'skin' to be defined", profileEl);
      }

      String removedLayers =
          getParser().string(profileEl, "remove-layers").attr().orNull();
      if (removedLayers != null) {
        layers = SkinPart.SkinLayers.allOf().minus(SkinPart.SkinLayers.parse(removedLayers));
      }
      pose = getParser().parseEnum(MannequinPose.class, profileEl, "pose").orNull();
    }

    FeatureReference<WaypointDefinition> waypoint =
        getParser().reference(WaypointDefinition.class, el, "waypoint").orNull();
    Boolean removeWaypoint = getParser().parseBool(el, "remove-waypoint").attr().orNull();
    if (waypoint != null && removeWaypoint != null && removeWaypoint) {
      throw new InvalidXMLException("'waypoint' and 'remove-waypoint' cannot be combined", el);
    }

    Formula<B> xFormula = null, yFormula = null, zFormula = null;
    Optional<Formula<B>> yawFormula = Optional.empty(), pitchFormula = Optional.empty();
    Element teleportEl = el.getChild("teleport");
    if (teleportEl != null) {
      xFormula = getParser().formula(scope, teleportEl, "x").orNull();
      yFormula = getParser().formula(scope, teleportEl, "y").orNull();
      zFormula = getParser().formula(scope, teleportEl, "z").orNull();
      yawFormula = getParser().formula(scope, teleportEl, "yaw").optional();
      pitchFormula = getParser().formula(scope, teleportEl, "pitch").optional();

      boolean any = xFormula != null || yFormula != null || zFormula != null;
      boolean all = xFormula != null && yFormula != null && zFormula != null;
      if (any && !all) {
        throw new InvalidXMLException("'x', 'y', and 'z' must all be specified", teleportEl);
      }
      boolean rotation = yawFormula.isPresent() || pitchFormula.isPresent();
      if (rotation && !all) {
        throw new InvalidXMLException(
            "'yaw'/'pitch' require 'x', 'y' and 'z' to be defined", teleportEl);
      }
    }

    return new ModifyMannequinAction<>(
        scope,
        mannequin,
        name,
        description,
        hideDescription,
        hideTitles,
        health,
        silent,
        invulnerable,
        glowing,
        immovable,
        gravity,
        physics,
        onFire,
        uuid,
        skin,
        layers,
        pose,
        waypoint,
        removeWaypoint,
        xFormula,
        yFormula,
        zFormula,
        yawFormula,
        pitchFormula);
  }
}
