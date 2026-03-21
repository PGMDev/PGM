package tc.oc.pgm.platform.sportpaper.material;

import java.util.Locale;
import org.bukkit.Material;
import org.bukkit.material.MaterialData;
import org.jspecify.annotations.Nullable;
import tc.oc.pgm.util.StringUtils;
import tc.oc.pgm.util.xml.InvalidXMLException;
import tc.oc.pgm.util.xml.Node;
import tc.oc.pgm.util.xml.XMLUtils;

class SpMaterialParser {

  public static MaterialData parseBukkit(Node node) throws InvalidXMLException {
    return parse(node.getValueNormalize(), node, false, true, Adapter.BUKKIT);
  }

  public static SpMaterialData parsePgm(String text, Node node, boolean forItem)
      throws InvalidXMLException {
    return parse(text, node, false, forItem, Adapter.PGM);
  }

  public static Material parseMaterial(String text, Node node, boolean forItem)
      throws InvalidXMLException {
    int id = StringUtils.parseNumericId(text);
    if (id != -1) {
      var byId = Material.getMaterial(id);
      if (byId == null)
        throw new InvalidXMLException("Could not find material with id '" + text + "'.", node);
      return byId;
    }

    text = normalize(text);

    var material = Material.getMaterial(text);
    var modern = ModernMaterialNames.get(text);

    if (modern != null) {
      // Always use the modern material name if it exists.
      // This permits materials with distinct item and block variants
      // in legacy to be matched to the correct variant always.
      if (material == null || modern.item().getItemType() != modern.block().getItemType()) {
        return forItem ? modern.item().getItemType() : modern.block().getItemType();
      }
    }

    if (material != null) return material;

    throw new InvalidXMLException("Could not find material '" + text + "'.", node);
  }

  public static <T> T parse(
      String text, @Nullable Node node, boolean matOnly, boolean forItem, Adapter<T> adapter)
      throws InvalidXMLException {
    if (matOnly) return adapter.visit(parseMaterial(text, node, forItem));

    String[] pieces = text.split(":");
    if (pieces.length > 2) {
      throw new InvalidXMLException("Invalid material pattern '" + text + "'.", node);
    }

    Material material = parseMaterial(pieces[0], node, forItem);
    if (pieces.length == 2) {
      try {
        return adapter.visit(material, XMLUtils.parseNumber(node, pieces[1], Short.class));
      } catch (NumberFormatException e) {
        throw new InvalidXMLException("Invalid damage value: " + pieces[1], node, e);
      }
    }

    String normalized = normalize(pieces[0]);
    if (Material.getMaterial(normalized) == null) {
      var mapping = ModernMaterialNames.get(normalized);
      if (mapping != null) {
        var md = forItem ? mapping.item() : mapping.block();
        if (md.getData() != 0) {
          return adapter.visit(md.getItemType(), md.getData());
        }
      }
    }

    return adapter.visit(material);
  }

  private static String normalize(String text) {
    return text.toUpperCase(Locale.ROOT).replaceAll("\\s+", "_").replaceAll("\\W", "");
  }

  interface Adapter<T> {
    Adapter<MaterialData> BUKKIT = new Adapter<>() {
      @Override
      public MaterialData visit(Material material) {
        return new MaterialData(material);
      }

      @SuppressWarnings("deprecation")
      @Override
      public MaterialData visit(Material material, short data) {
        return new MaterialData(material, (byte) data);
      }
    };

    Adapter<SpMaterialData> PGM = new Adapter<>() {
      @Override
      public SpMaterialData visit(Material material) {
        return new SpMaterialData(material, (short) 0);
      }

      @Override
      public SpMaterialData visit(Material material, short data) {
        return new SpMaterialData(material, data);
      }
    };

    T visit(Material material);

    T visit(Material material, short data);
  }
}
