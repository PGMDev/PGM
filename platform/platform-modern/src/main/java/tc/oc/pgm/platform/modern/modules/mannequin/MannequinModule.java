package tc.oc.pgm.platform.modern.modules.mannequin;

import com.google.common.collect.ImmutableMap;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Logger;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.inventory.MainHand;
import org.jdom2.Document;
import org.jdom2.Element;
import tc.oc.pgm.action.Action;
import tc.oc.pgm.api.map.MapModule;
import tc.oc.pgm.api.map.factory.MapFactory;
import tc.oc.pgm.api.map.factory.MapModuleFactory;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.platform.modern.modules.behavior.BehaviorDefinition;
import tc.oc.pgm.platform.modern.modules.waypoint.WaypointDefinition;
import tc.oc.pgm.util.skin.Skin;
import tc.oc.pgm.util.xml.InvalidXMLException;
import tc.oc.pgm.util.xml.Node;
import tc.oc.pgm.util.xml.XMLUtils;

public record MannequinModule(Map<String, MannequinDefinition> mannequinDefinitions)
    implements MapModule<MannequinMatchModule> {

  @Override
  public MannequinMatchModule createMatchModule(Match match) {
    return new MannequinMatchModule(match, mannequinDefinitions);
  }

  public static class Factory implements MapModuleFactory<MannequinModule> {
    @Override
    public MannequinModule parse(MapFactory factory, Logger logger, Document doc)
        throws InvalidXMLException {
      Map<String, MannequinDefinition> mannequins = new HashMap<>();
      var parser = factory.getParser();

      for (Element el : XMLUtils.flattenElements(doc.getRootElement(), "mannequins", "mannequin")) {
        String id = parser.string(el, "id").required();
        Component name = XMLUtils.parseFormattedText(Node.fromChildOrAttr(el, "name"));
        Component description =
            XMLUtils.parseFormattedText(Node.fromChildOrAttr(el, "description"));
        boolean silent = parser.parseBool(el, "silent").attr().optional(false);
        boolean invulnerable = parser.parseBool(el, "invulnerable").attr().optional(false);
        TextColor glowing = parser.textColor(el, "glowing").orNull();
        float health = parser.parseFloat(el, "health").attr().optional(20f);
        boolean immovable = parser.parseBool(el, "immovable").attr().optional(false);
        MainHand mainHand =
            parser.parseEnum(MainHand.class, el, "main-hand").optional(MainHand.RIGHT);
        boolean gravity = parser.parseBool(el, "gravity").attr().optional(false);
        boolean physics = parser.parseBool(el, "physics").attr().optional(true);
        boolean onFire = parser.parseBool(el, "on-fire").attr().optional(false);

        Element profileEl = el.getChild("profile");
        if (profileEl == null) {
          throw new InvalidXMLException(
              "Mannequin '" + id + "' is missing its required <profile> sub-element.", el);
        }
        UUID uuid = XMLUtils.parseUuid(Node.fromRequiredAttr(profileEl, "uuid"));
        Skin skin = XMLUtils.parseUnsignedSkin(Node.fromRequiredChildOrAttr(profileEl, "skin"));
        MannequinPose pose = parser
            .parseEnum(MannequinPose.class, profileEl, "pose")
            .optional(MannequinPose.STANDING);
        SkinPart.SkinLayers layers = SkinPart.SkinLayers.allOf();
        String removedLayers = parser.string(profileEl, "remove-layers").attr().orNull();
        if (removedLayers != null) {
          layers = SkinPart.SkinLayers.allOf().minus(SkinPart.SkinLayers.parse(removedLayers));
        }

        Action<? super MatchPlayer> action =
            parser.action(MatchPlayer.class, el, "click-action").orNull();

        var waypoint =
            parser.reference(WaypointDefinition.class, el, "waypoint").orNull();

        var behavior =
            parser.reference(BehaviorDefinition.class, el, "behavior").orNull();

        MannequinDefinition mannequinDefinition = new MannequinDefinition(
            id,
            name,
            description,
            uuid,
            skin,
            silent,
            invulnerable,
            glowing,
            health,
            pose,
            immovable,
            mainHand,
            gravity,
            physics,
            onFire,
            layers,
            action,
            waypoint,
            behavior);

        factory.getFeatures().addFeature(el, mannequinDefinition);
        mannequins.put(id, mannequinDefinition);
      }

      return mannequins.isEmpty() ? null : new MannequinModule(ImmutableMap.copyOf(mannequins));
    }
  }
}
