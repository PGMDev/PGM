package tc.oc.pgm.platform.v1_20_6.material;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import java.util.HashSet;
import java.util.Set;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.UnsafeValues;
import org.bukkit.craftbukkit.util.CraftMagicNumbers;
import org.bukkit.material.MaterialData;
import org.jetbrains.annotations.Nullable;
import tc.oc.pgm.util.material.BlockMaterialData;
import tc.oc.pgm.util.material.ItemMaterialData;
import tc.oc.pgm.util.xml.InvalidXMLException;
import tc.oc.pgm.util.xml.Node;
import tc.oc.pgm.util.xml.XMLUtils;

class ModernMaterialParser {

  private static final UnsafeValues UNSAFE = Bukkit.getUnsafe();
  private static final Int2ObjectMap<Material> BY_ID = new Int2ObjectOpenHashMap<>(400);

  static {
    for (Material value : Material.values()) {
      if (value.isLegacy()) BY_ID.put(value.getId(), value);
    }
  }

  public static BlockMaterialData parseBlock(String text, Node node) throws InvalidXMLException {
    return parse(text, node, false, Adapter.PGM_BLOCK);
  }

  public static ItemMaterialData parseItem(String text, Node node) throws InvalidXMLException {
    var res = parse(text, node, false, Adapter.PGM_ITEM);
    if (CraftMagicNumbers.getItem(res.getItemType()) == null) {
      throw new InvalidXMLException("Invalid item/block", node);
    }
    return res;
  }

  public static ItemMaterialData parseItem(String text, short dmg, Node node)
      throws InvalidXMLException {
    var res = Adapter.PGM_ITEM.visit(parseMaterial(text, node), dmg);
    if (CraftMagicNumbers.getItem(res.getItemType()) == null) {
      throw new InvalidXMLException("Invalid item/block", node);
    }
    return res;
  }

  public static ItemMaterialData parseItem(Material material, short dmg) {
    return Adapter.PGM_ITEM.visit(material, dmg);
  }

  public static Material[] parseFlatten(Node node) throws InvalidXMLException {
    return parse(node.getValueNormalize(), node, false, Adapter.FLATTENING);
  }

  public static Material[] flatten(Material material) {
    if (!material.isLegacy()) return new Material[] {material};
    Set<Material> materials = new HashSet<>(16);
    materials.add(UNSAFE.fromLegacy(new org.bukkit.material.MaterialData(material, (byte) 0)));
    materials.add(UNSAFE.fromLegacy(new org.bukkit.material.MaterialData(material, (byte) 1)));
    if (materials.size() == 2) {
      for (byte i = 2; i < 16; i++) {
        materials.add(UNSAFE.fromLegacy(new org.bukkit.material.MaterialData(material, i)));
      }
    }
    return materials.toArray(Material[]::new);
  }

  private static int materialId(String text) {
    return switch (text.length()) {
      default -> -1;
      case 1 -> Character.digit(text.charAt(0), 10);
      case 2 -> {
        int a = Character.digit(text.charAt(0), 10);
        if (a == -1) yield -1;
        int b = Character.digit(text.charAt(1), 10);
        yield Math.min(a, b) == -1 ? -1 : ((a * 10) + b);
      }
      case 3 -> {
        int a = Character.digit(text.charAt(0), 10);
        if (a == -1) yield -1;
        int b = Character.digit(text.charAt(1), 10);
        int c = Character.digit(text.charAt(1), 10);
        yield Math.min(b, c) == -1 ? -1 : ((a * 10) + b);
      }
    };
  }

  public static Material parseMaterial(String text, Node node) throws InvalidXMLException {
    int id = materialId(text);
    Material legacy;
    if (id != -1) {
      legacy = BY_ID.get(id);
      if (legacy == null)
        throw new InvalidXMLException("Could not find material with id '" + text + "'.", node);
    } else {
      legacy = Material.matchMaterial(text, true);
    }
    if (legacy != null) return legacy;

    Material modern = Material.matchMaterial(text);
    if (modern == null) {
      throw new InvalidXMLException("Could not find material '" + text + "'.", node);
    }
    return modern;
  }

  public static <T> T parse(String text, @Nullable Node node, boolean matOnly, Adapter<T> adapter)
      throws InvalidXMLException {
    if (matOnly) return adapter.visit(parseMaterial(text, node));

    String[] pieces = text.split(":");
    if (pieces.length > 2) {
      throw new InvalidXMLException("Invalid material pattern '" + text + "'.", node);
    }

    Material material = parseMaterial(pieces[0], node);
    if (pieces.length == 1) {
      return adapter.visit(material);
    } else {
      try {
        return adapter.visit(material, XMLUtils.parseNumber(node, pieces[1], Short.class));
      } catch (NumberFormatException e) {
        throw new InvalidXMLException("Invalid damage value: " + pieces[1], node, e);
      }
    }
  }

  public interface Adapter<T> {
    Adapter<BlockMaterialData> PGM_BLOCK = new Adapter<>() {
      @Override
      public BlockMaterialData visit(Material material) {
        return new ModernBlockData(material.createBlockData());
      }

      @Override
      public BlockMaterialData visit(Material material, short data) {
        return new ModernBlockData(UNSAFE.fromLegacy(material, (byte) data));
      }
    };

    Adapter<ItemMaterialData> PGM_ITEM = new Adapter<>() {
      @Override
      public ItemMaterialData visit(Material material) {
        return new ModernItemData(UNSAFE.fromLegacy(material));
      }

      @Override
      public ItemMaterialData visit(Material material, short data) {
        return new ModernItemData(UNSAFE.fromLegacy(new MaterialData(material, (byte) data), true));
      }
    };

    Adapter<Material[]> FLATTENING = new Adapter<>() {
      @Override
      public Material[] visit(Material material) {
        return flatten(material);
      }

      @Override
      public Material[] visit(Material material, short data) {
        return new Material[] {UNSAFE.fromLegacy(new MaterialData(material, (byte) data))};
      }
    };

    T visit(Material material);

    T visit(Material material, short data);
  }
}
