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
    return parse(node.getValueNormalize(), node, false, Adapter.BUKKIT);
  }

  public static SpMaterialData parsePgm(String text, Node node) throws InvalidXMLException {
    return parse(text, node, false, Adapter.PGM);
  }

  public static Material parseMaterial(String text, Node node) throws InvalidXMLException {
    int id = StringUtils.parseNumericId(text);
    if (id != -1) {
      var byId = Material.getMaterial(id);
      if (byId == null)
        throw new InvalidXMLException("Could not find material with id '" + text + "'.", node);
      return byId;
    }

    text = normalize(text);

    // At some point prior to legacy, this rename happened
    if (text.equals("SNOWBALL")) return Material.SNOW_BALL;

    var material = Material.getMaterial(text);
    if (material != null) return material;

    var modern = ModernMaterialNames.get(text);
    if (modern != null) return modern.getItemType();

    throw new InvalidXMLException("Could not find material '" + text + "'.", node);
  }

  public static <T> T parse(String text, @Nullable Node node, boolean matOnly, Adapter<T> adapter)
      throws InvalidXMLException {
    if (matOnly) return adapter.visit(parseMaterial(text, node));

    String[] pieces = text.split(":");
    if (pieces.length > 2) {
      throw new InvalidXMLException("Invalid material pattern '" + text + "'.", node);
    }

    Material material = parseMaterial(pieces[0], node);
    if (pieces.length == 2) {
      try {
        return adapter.visit(material, XMLUtils.parseNumber(node, pieces[1], Short.class));
      } catch (NumberFormatException e) {
        throw new InvalidXMLException("Invalid damage value: " + pieces[1], node, e);
      }
    }

    String normalized = normalize(pieces[0]);
    if (Material.getMaterial(normalized) == null) {
      var modern = ModernMaterialNames.get(normalized);
      if (modern != null && modern.getData() != 0) {
        return adapter.visit(modern.getItemType(), modern.getData());
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
