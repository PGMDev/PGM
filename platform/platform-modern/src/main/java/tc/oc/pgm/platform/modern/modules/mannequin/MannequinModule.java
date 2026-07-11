package tc.oc.pgm.platform.modern.modules.mannequin;

import com.google.common.collect.ImmutableMap;
import net.kyori.adventure.text.format.TextColor;
import org.jdom2.Document;
import org.jdom2.Element;
import tc.oc.pgm.api.map.MapModule;
import tc.oc.pgm.api.map.factory.MapFactory;
import tc.oc.pgm.api.map.factory.MapModuleFactory;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.util.skin.Skin;
import tc.oc.pgm.util.xml.InvalidXMLException;
import tc.oc.pgm.util.xml.Node;
import tc.oc.pgm.util.xml.XMLUtils;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Logger;

public class MannequinModule implements MapModule<MannequinMatchModule> {
  private final Map<String, MannequinDefinition> mannequinDefinitions;

  public MannequinModule(Map<String, MannequinDefinition> mannequinDefinitions) {
    this.mannequinDefinitions = mannequinDefinitions;
  }

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

      for (Element el :
          XMLUtils.flattenElements(doc.getRootElement(), "mannequins", "mannequin")) {
        String id = parser.string(el, "id").required();
        String name = parser.string(el, "name").orNull();
        boolean silent = parser.parseBool(el, "silent").attr().optional(false);
        boolean invulnerable = parser.parseBool(el, "invulnerable").attr().optional(false);
        TextColor glowing = parser.textColor(el, "glowing").orNull();
        float health = parser.parseFloat(el, "health").attr().optional(20f);
        boolean immovable = parser.parseBool(el, "immovable").attr().optional(false);

        Element profileEl = el.getChild("profile");
        if (profileEl == null) {
          throw new InvalidXMLException(
              "Mannequin '" + id + "' is missing its required <profile> sub-element.", el);
        }
        UUID uuid = XMLUtils.parseUuid(Node.fromRequiredAttr(profileEl, "uuid"));
        Skin skin = XMLUtils.parseUnsignedSkin(Node.fromRequiredChildOrAttr(profileEl, "skin"));
        MannequinPose pose = parser.parseEnum(MannequinPose.class, profileEl, "pose").optional(MannequinPose.STANDING);
        SkinPart.SkinLayers layers = SkinPart.SkinLayers.allOf();
        String removedLayers = parser.string(profileEl, "remove-layers").attr().orNull();
        if (removedLayers != null) {
          layers = SkinPart.SkinLayers.allOf().minus(SkinPart.SkinLayers.parse(removedLayers));
        }

        MannequinDefinition mannequinDefinition = new MannequinDefinition(
            id,
            name,
            uuid,
            skin,
            silent,
            invulnerable,
            glowing,
            health,
            pose,
            immovable,
            layers);

        factory.getFeatures().addFeature(el, mannequinDefinition);
        mannequins.put(id, mannequinDefinition);
      }

      return mannequins.isEmpty() ? null : new MannequinModule(ImmutableMap.copyOf(mannequins));
    }
  }
}
