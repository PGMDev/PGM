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

  public static SpMaterialData parseItem(String text, Node node) throws InvalidXMLException {
    return parse(text, node, false, Adapter.PGM_ITEM);
  }

  public static SpMaterialData parseBlock(String text, Node node) throws InvalidXMLException {
    return parse(text, node, false, Adapter.PGM_BLOCK);
  }

  public static Material parseMaterial(String text, Node node) throws InvalidXMLException {
    // The set of legacy-mapped modern materials which have separate item
    // and block forms do not consist of blocks that can be gotten as items
    // legitimately or easily, so for shop currency we force the item here.
    return parse(text, node, true, Adapter.PGM_BLOCK).getItemType();
  }

  public static <T> T parse(String text, @Nullable Node node, boolean matOnly, Adapter<T> adapter)
      throws InvalidXMLException {
    if (matOnly) return parseMaterial(normalize(text), node, adapter);

    String[] pieces = text.split(":");
    if (pieces.length > 2)
      throw new InvalidXMLException("Invalid material pattern '" + text + "'.", node);

    String head = normalize(pieces[0]);
    if (pieces.length == 1) return parseMaterial(head, node, adapter);

    try {
      return adapter.visit(
          resolveMaterial(head, node), XMLUtils.parseNumber(node, pieces[1], Short.class));
    } catch (NumberFormatException e) {
      throw new InvalidXMLException("Invalid damage value: " + pieces[1], node, e);
    }
  }

  @SuppressWarnings("deprecation")
  private static Material resolveMaterial(String text, Node node) throws InvalidXMLException {
    int id = StringUtils.parseNumericId(text);
    if (id != -1) {
      var byId = Material.getMaterial(id);
      if (byId == null)
        throw new InvalidXMLException("Could not find material with id '" + text + "'.", node);
      return byId;
    }

    var material = Material.getMaterial(text);
    if (material != null) return material;

    var modern = ModernMaterialNames.get(text);
    if (modern != null) return modern.blockType();

    throw new InvalidXMLException("Could not find material '" + text + "'.", node);
  }

  @SuppressWarnings("deprecation")
  private static <T> T parseMaterial(String text, Node node, Adapter<T> adapter)
      throws InvalidXMLException {
    int id = StringUtils.parseNumericId(text);
    if (id != -1) {
      var byId = Material.getMaterial(id);
      if (byId == null)
        throw new InvalidXMLException("Could not find material with id '" + text + "'.", node);
      return adapter.visit(byId);
    }

    var material = Material.getMaterial(text);
    var modern = ModernMaterialNames.get(text);

    // Always parse modern item name if existent
    if (modern != null && (material == null || !modern.single() || modern.type() != material)) {
      return adapter.visit(modern);
    }

    if (material != null) return adapter.visit(material);

    throw new InvalidXMLException("Could not find material '" + text + "'.", node);
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

      @Override
      public MaterialData visit(ModernMaterialNames.MaterialMapping mapping) {
        return mapping
            .blockData()
            .map(d -> new MaterialData(mapping.blockType(), (byte) (short) d))
            .orElseGet(() -> new MaterialData(mapping.blockType()));
      }
    };

    Adapter<SpMaterialData> PGM_ITEM = new Adapter<>() {
      @Override
      public SpMaterialData visit(Material material) {
        return new SpMaterialData(material);
      }

      @Override
      public SpMaterialData visit(Material material, short data) {
        return new SpMaterialData(material, data);
      }

      @Override
      public SpMaterialData visit(ModernMaterialNames.MaterialMapping mapping) {
        return mapping
            .itemData()
            .map(data -> new SpMaterialData(mapping.itemType(), data))
            .orElseGet(() -> new SpMaterialData(mapping.itemType()));
      }
    };

    Adapter<SpMaterialData> PGM_BLOCK = new Adapter<>() {
      @Override
      public SpMaterialData visit(Material material) {
        return new SpMaterialData(material);
      }

      @Override
      public SpMaterialData visit(Material material, short data) {
        return new SpMaterialData(material, data);
      }

      @Override
      public SpMaterialData visit(ModernMaterialNames.MaterialMapping mapping) {
        return mapping
            .blockData()
            .map(d -> new SpMaterialData(mapping.blockType(), d))
            .orElseGet(() -> new SpMaterialData(mapping.blockType()));
      }
    };

    T visit(Material material);

    T visit(Material material, short data);

    T visit(ModernMaterialNames.MaterialMapping mapping);
  }
}
