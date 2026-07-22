package tc.oc.pgm.platform.modern.action;

import java.util.Optional;
import java.util.UUID;
import net.kyori.adventure.text.Component;
import org.jdom2.Element;
import tc.oc.pgm.action.ActionParser;
import tc.oc.pgm.api.feature.FeatureReference;
import tc.oc.pgm.api.map.factory.MapFactory;
import tc.oc.pgm.filters.Filterable;
import tc.oc.pgm.platform.modern.action.actions.ModifyMannequinAction;
import tc.oc.pgm.platform.modern.action.actions.SpawnMannequinAction;
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

  @MethodParser("spawn-mannequin")
  public <B extends Filterable<?>> SpawnMannequinAction<B> parseSpawnMannequin(
      Element el, Class<B> scope) throws InvalidXMLException {
    scope = parseScope(el, scope);
    var mannequin = parser.reference(MannequinDefinition.class, el, "mannequin").required();
    var xformula = parser.formula(scope, el, "x").required();
    var yformula = parser.formula(scope, el, "y").required();
    var zformula = parser.formula(scope, el, "z").required();
    var yawFormula = parser.formula(scope, el, "yaw").optional();
    var pitchFormula = parser.formula(scope, el, "pitch").optional();

    return new SpawnMannequinAction<>(
        scope, mannequin, xformula, yformula, zformula, yawFormula, pitchFormula);
  }

  @MethodParser("modify-mannequin")
  public <B extends Filterable<?>> ModifyMannequinAction<B> parseModifyMannequin(
      Element el, Class<B> scope) throws InvalidXMLException {
    scope = parseScope(el, scope);
    var mannequin = parser.reference(MannequinDefinition.class, el, "mannequin").required();
    Boolean despawn = parser.parseBool(el, "despawn").orFalse();
    Component name = XMLUtils.parseFormattedText(Node.fromChildOrAttr(el, "name"));
    Component description = XMLUtils.parseFormattedText(Node.fromChildOrAttr(el, "description"));
    Boolean hideDescription = parser.parseBool(el, "hide-description").attr().orNull();
    Boolean hideTitles = parser.parseBool(el, "hide-titles").attr().orNull();
    Float health = parser.parseFloat(el, "health").attr().orNull();
    Boolean silent = parser.parseBool(el, "silent").attr().orNull();
    Boolean invulnerable = parser.parseBool(el, "invulnerable").attr().orNull();
    Boolean glowing = parser.parseBool(el, "glowing").orFalse();
    Boolean immovable = parser.parseBool(el, "immovable").attr().orNull();
    Boolean gravity = parser.parseBool(el, "gravity").attr().orNull();
    Boolean physics = parser.parseBool(el, "physics").attr().orNull();
    Boolean onFire = parser.parseBool(el, "on-fire").attr().orNull();

    boolean playerProfile = false;
    UUID uuid = null;
    Skin skin = null;
    SkinPart.SkinLayers layers = null;
    MannequinPose pose = null;
    Element profileEl = el.getChild("profile");
    if (profileEl != null) {
      Node uuidNode = Node.fromAttr(profileEl, "uuid");
      Node skinNode = Node.fromChildOrAttr(profileEl, "skin");
      if (uuidNode != null && skinNode != null) {
        playerProfile = "#player#".equals(uuidNode.getValueNormalize())
            || "#player#".equals(skinNode.getValueNormalize());
        if (playerProfile
            && !("#player#".equals(uuidNode.getValueNormalize())
                && "#player#".equals(skinNode.getValueNormalize()))) {
          throw new InvalidXMLException(
              "'uuid' and 'skin' must both be '#player#' or both be regularly defined", profileEl);
        }
        if (!playerProfile) {
          uuid = XMLUtils.parseUuid(uuidNode);
          skin = XMLUtils.parseUnsignedSkin(skinNode);
        }
      } else if (uuidNode != null || skinNode != null) {
        throw new InvalidXMLException(
            "Skin changes require both 'uuid' and 'skin' to be defined", profileEl);
      }

      String removedLayers = parser.string(profileEl, "remove-layers").attr().orNull();
      if (removedLayers != null) {
        layers = SkinPart.SkinLayers.allOf().minus(SkinPart.SkinLayers.parse(removedLayers));
      }
      pose = parser.parseEnum(MannequinPose.class, profileEl, "pose").orNull();
    }

    FeatureReference<WaypointDefinition> waypoint =
        parser.reference(WaypointDefinition.class, el, "waypoint").orNull();
    Boolean removeWaypoint = parser.parseBool(el, "remove-waypoint").attr().orNull();
    if (waypoint != null && removeWaypoint != null && removeWaypoint) {
      throw new InvalidXMLException("'waypoint' and 'remove-waypoint' cannot be combined", el);
    }

    Formula<B> xFormula = null, yFormula = null, zFormula = null;
    Optional<Formula<B>> yawFormula = Optional.empty(), pitchFormula = Optional.empty();
    Element teleportEl = el.getChild("teleport");
    if (teleportEl != null) {
      xFormula = parser.formula(scope, teleportEl, "x").orNull();
      yFormula = parser.formula(scope, teleportEl, "y").orNull();
      zFormula = parser.formula(scope, teleportEl, "z").orNull();
      yawFormula = parser.formula(scope, teleportEl, "yaw").optional();
      pitchFormula = parser.formula(scope, teleportEl, "pitch").optional();

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
        despawn,
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
        playerProfile,
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
