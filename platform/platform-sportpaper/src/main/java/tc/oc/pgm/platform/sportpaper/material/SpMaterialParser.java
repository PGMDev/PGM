package tc.oc.pgm.platform.sportpaper.material;

import java.util.Locale;
import org.bukkit.Material;
import org.bukkit.material.MaterialData;
import org.jspecify.annotations.Nullable;
import tc.oc.pgm.platform.sportpaper.material.ModernMaterialNames.MaterialMapping;
import tc.oc.pgm.util.StringUtils;
import tc.oc.pgm.util.xml.InvalidXMLException;
import tc.oc.pgm.util.xml.Node;
import tc.oc.pgm.util.xml.XMLUtils;

class SpMaterialParser {

  public static MaterialData parseBukkitItem(Node node) throws InvalidXMLException {
    return parse(node.getValueNormalize(), node, false, Adapter.BUKKIT_ITEM);
  }

  public static SpMaterialData parseItem(String text, Node node) throws InvalidXMLException {
    return parse(text, node, false, Adapter.PGM_ITEM);
  }

  public static SpMaterialData parseBlock(String text, Node node) throws InvalidXMLException {
    return parse(text, node, false, Adapter.PGM_BLOCK);
  }

  public static Material parseMaterial(String text, Node node) throws InvalidXMLException {
    return parse(text, node, true, Adapter.MATERIAL);
  }

  @SuppressWarnings("deprecation")
  public static <T> T parse(String text, @Nullable Node node, boolean matOnly, Adapter<T> adapter)
      throws InvalidXMLException {
    String head = text;
    Short data = null;
    if (!matOnly) {
      // If we're parsing more than just material, split material:data
      String[] pieces = text.split(":");
      if (pieces.length > 2)
        throw new InvalidXMLException("Invalid material pattern '" + text + "'.", node);

      head = pieces[0];
      if (pieces.length == 2) data = XMLUtils.parseNumber(node, pieces[1], Short.class);
    }

    // Attempt parsing as numberic id
    int id = StringUtils.parseNumericId(head);
    if (id != -1) {
      var byId = Material.getMaterial(id);
      if (byId == null)
        throw new InvalidXMLException("Could not find material with id '" + head + "'.", node);
      return data != null ? adapter.visit(byId, data) : adapter.visit(byId);
    }

    head = normalize(head);

    // Attempt parsing as modern name, only if no data
    if (data == null) {
      var modern = ModernMaterialNames.get(head);
      if (modern != null) return adapter.visit(node, modern);
    }

    // Parse as legacy material name
    var material = Material.getMaterial(head);
    if (material != null)
      return data != null ? adapter.visit(material, data) : adapter.visit(material);

    throw new InvalidXMLException("Could not find material '" + text + "'.", node);
  }

  private static String normalize(String text) {
    return text.toUpperCase(Locale.ROOT).replaceAll("\\s+", "_").replaceAll("\\W", "");
  }

  interface Adapter<T> {
    Adapter<MaterialData> BUKKIT_ITEM = new Adapter<>() {
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
      public MaterialData visit(Node node, MaterialMapping mapping) throws InvalidXMLException {
        return mapping.mapItem(node, this::visit, this::visit);
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
      public SpMaterialData visit(Node node, MaterialMapping mapping) throws InvalidXMLException {
        return mapping.mapItem(node, this::visit, this::visit);
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
      public SpMaterialData visit(Node node, MaterialMapping mapping) throws InvalidXMLException {
        return mapping.mapBlock(node, this::visit, this::visit);
      }
    };

    Adapter<Material> MATERIAL = new Adapter<>() {
      @Override
      public Material visit(Material material) {
        return material;
      }

      @Override
      public Material visit(Material material, short data) {
        // Should never happen, parsing does material-only and remapping should throw earlier.
        throw new UnsupportedOperationException(
            "Only supports materials, but got " + material + ":" + data);
      }

      @Override
      public Material visit(Node node, MaterialMapping mapping) throws InvalidXMLException {
        var material = mapping.mapSingle(node, (m, d) -> null, this::visit);
        if (material == null)
          throw new InvalidXMLException(
              "Mapping does not have a material-only form " + mapping, node);
        return material;
      }
    };

    T visit(Material material);

    T visit(Material material, short data);

    T visit(Node node, MaterialMapping mapping) throws InvalidXMLException;
  }
}
