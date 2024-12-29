package tc.oc.pgm.platform.modern.material;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.UnsafeValues;
import org.bukkit.craftbukkit.legacy.CraftLegacy;
import org.bukkit.craftbukkit.util.CraftMagicNumbers;
import org.jetbrains.annotations.Nullable;
import tc.oc.pgm.util.material.BlockMaterialData;
import tc.oc.pgm.util.material.ItemMaterialData;
import tc.oc.pgm.util.material.Materials;
import tc.oc.pgm.util.xml.InvalidXMLException;
import tc.oc.pgm.util.xml.Node;
import tc.oc.pgm.util.xml.XMLUtils;

@SuppressWarnings({"deprecation", "removal", "UnstableApiUsage"})
class ModernMaterialParser {

  private static final UnsafeValues UNSAFE = Bukkit.getUnsafe();
  private static final Int2ObjectMap<Material> BY_ID = new Int2ObjectOpenHashMap<>(400);

  static {
    for (Material value : CraftLegacy.values()) {
      BY_ID.put(value.getId(), value);
    }
  }

  public static BlockMaterialData parseBlock(String text, Node node) throws InvalidXMLException {
    return parse(text, node, false, Adapter.PGM_BLOCK);
  }

  public static ItemMaterialData parseItem(String text, Node node) throws InvalidXMLException {
    var res = parse(text, node, false, Adapter.PGM_ITEM);
    validateItem(res.getItemType(), node);
    return res;
  }

  public static ItemMaterialData parseItem(String text, short dmg, Node node)
      throws InvalidXMLException {
    var res = Adapter.PGM_ITEM.visit(parseLegacyMaterial(text, node), dmg);
    validateItem(res.getItemType(), node);
    return res;
  }

  public static Material parseMaterial(String text, Node node) throws InvalidXMLException {
    return upgrade(parseLegacyMaterial(text, node), (short) 0);
  }

  static void validateItem(Material material, Node node) throws InvalidXMLException {
    if (CraftMagicNumbers.getItem(material) == null) {
      throw new InvalidXMLException("Invalid item/block " + material, node);
    }
  }

  public static ItemMaterialData parseItem(Material material, short dmg) {
    return Adapter.PGM_ITEM.visit(material, dmg);
  }

  public static Material[] parseFlatten(Node node) throws InvalidXMLException {
    return parse(node.getValueNormalize(), node, false, Adapter.FLATTENING);
  }

  public static Material[] flatten(Material material) {
    if (!material.isLegacy()) return new Material[] {material};
    var md = new org.bukkit.material.MaterialData(material, (byte) 0);
    var main = UNSAFE.fromLegacy(md);
    md.setData((byte) 1);
    var alt = UNSAFE.fromLegacy(md);
    if (main == alt || alt.isAir()) return new Material[] {main};

    Set<Material> materials = new HashSet<>(16);
    materials.add(main);
    materials.add(alt);
    for (byte i = 3; i < 16; i++) {
      md.setData(i);
      materials.add(UNSAFE.fromLegacy(md));
    }
    materials.remove(Material.AIR);
    return materials.toArray(Material[]::new);
  }

  private static Material parseLegacyMaterial(String text, Node node) throws InvalidXMLException {
    int id = Materials.materialId(text);
    if (id != -1) {
      var byId = BY_ID.get(id);
      if (byId == null)
        throw new InvalidXMLException("Could not find material with id '" + text + "'.", node);
      return byId;
    }
    text = text.toUpperCase(Locale.ROOT).replaceAll("\\s+", "_").replaceAll("\\W", "");

    var legacy = Material.getMaterial("LEGACY_" + text);
    if (legacy != null) return legacy;

    Material modern = Material.getMaterial(text);
    if (modern == null) {
      throw new InvalidXMLException("Could not find material '" + text + "'.", node);
    }
    return modern;
  }

  public static <T> T parse(String text, @Nullable Node node, boolean matOnly, Adapter<T> adapter)
      throws InvalidXMLException {
    if (matOnly) return adapter.visit(parseLegacyMaterial(text, node));

    String[] pieces = text.split(":");
    if (pieces.length > 2) {
      throw new InvalidXMLException("Invalid material pattern '" + text + "'.", node);
    }

    Material material = parseLegacyMaterial(pieces[0], node);
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

  private static Material upgrade(Material material, short data) {
    if (material.isLegacy()) {
      var newMat =
          UNSAFE.fromLegacy(new org.bukkit.material.MaterialData(material, (byte) data), true);
      // If material+data is invalid, do fromLegacy with just material
      return newMat.isAir() ? UNSAFE.fromLegacy(material) : newMat;
    }
    return material;
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
        return switch (material = upgrade(material, data)) {
          case POTION, SPLASH_POTION -> new PotionMaterialData(data);
          default -> new ModernItemData(material);
        };
      }
    };

    Adapter<Material[]> FLATTENING = new Adapter<>() {
      @Override
      public Material[] visit(Material material) {
        return flatten(material);
      }

      @Override
      public Material[] visit(Material material, short data) {
        return new Material[] {upgrade(material, data)};
      }
    };

    T visit(Material material);

    T visit(Material material, short data);
  }
}
