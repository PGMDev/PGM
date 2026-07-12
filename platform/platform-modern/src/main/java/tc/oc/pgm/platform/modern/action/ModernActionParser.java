package tc.oc.pgm.platform.modern.action;

import java.util.Optional;
import net.kyori.adventure.text.Component;
import org.jdom2.Element;
import tc.oc.pgm.action.ActionParser;
import tc.oc.pgm.api.map.factory.MapFactory;
import tc.oc.pgm.filters.Filterable;
import tc.oc.pgm.platform.modern.action.actions.ModifyMannequinAction;
import tc.oc.pgm.platform.modern.action.actions.SpawnMannequinAction;
import tc.oc.pgm.platform.modern.modules.mannequin.MannequinDefinition;
import tc.oc.pgm.platform.modern.modules.mannequin.MannequinPose;
import tc.oc.pgm.util.MethodParser;
import tc.oc.pgm.util.math.Formula;
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

    MannequinPose pose = null;
    Element profileEl = el.getChild("profile");
    if (profileEl != null) {
      pose = getParser().parseEnum(MannequinPose.class, profileEl, "pose").orNull();
      //      Add skin customization/rotation separate from teleport action later
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
      if ((any && !all) || (rotation && !all)) {
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
        pose,
        xFormula,
        yFormula,
        zFormula,
        yawFormula,
        pitchFormula);
  }
}
