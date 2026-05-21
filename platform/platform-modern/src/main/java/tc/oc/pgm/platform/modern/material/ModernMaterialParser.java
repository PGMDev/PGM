package tc.oc.pgm.platform.modern.material;

import com.google.common.collect.HashMultimap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import net.minecraft.world.level.block.state.BlockState;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.UnsafeValues;
import org.bukkit.craftbukkit.legacy.CraftLegacy;
import org.bukkit.craftbukkit.util.CraftMagicNumbers;
import org.jetbrains.annotations.Nullable;
import tc.oc.pgm.util.StringUtils;
import tc.oc.pgm.util.material.BlockMaterialData;
import tc.oc.pgm.util.material.ItemMaterialData;
import tc.oc.pgm.util.xml.InvalidXMLException;
import tc.oc.pgm.util.xml.Node;
import tc.oc.pgm.util.xml.XMLUtils;

@SuppressWarnings({"deprecation", "removal", "UnstableApiUsage"})
class ModernMaterialParser {

  private static final UnsafeValues UNSAFE = Bukkit.getUnsafe();
  private static final Int2ObjectMap<Material> BY_ID = new Int2ObjectOpenHashMap<>(400);
  private static final int[] UPGRADE_COUNT = new int[256];

  static {
    HashMultimap<Material, BlockState> reachableStates = HashMultimap.create(256, 1);
    for (Material value : CraftLegacy.values()) {
      BY_ID.put(value.getId(), value);

      // Legacy blocks 0..255, some legacy items may also respond true to isBlock
      if (value.getId() >= 256) continue;

      for (byte i = 0; i < 16; i++) {
        var state = CraftLegacy.fromLegacyData(value, i);
        if (!state.isAir()) reachableStates.put(state.getBukkitMaterial(), state);
      }
    }

    for (Material value : CraftLegacy.values()) {
      if (value.getId() >= 256) break;

      // Compute for each material:data if we should match as material, blockstate, or a mix.
      int result = 0;
      for (byte i = 0; i < 16; i++) {
        var state = CraftLegacy.fromLegacyData(value, i);
        if (state.isAir()) continue;

        var reachable = reachableStates.get(state.getBukkitMaterial()).size();
        if (reachable == 1) continue; // no need to write USE_MATERIAL(0)
        var modern = state.getBlock().getStateDefinition().getPossibleStates().size();
        if (modern == 1) continue; // no need to write USE_MATERIAL(0)

        result |= (reachable == modern ? UpgradeStrat.BLOCK_STATE : UpgradeStrat.GOOD_LUCK).to(i);
      }
      UPGRADE_COUNT[value.getId()] = result;
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
    String[] pieces = text.split(":");
    if (pieces.length > 2) {
      throw new InvalidXMLException("Invalid material pattern '" + text + "'.", node);
    }

    Material material = parseLegacyMaterial(pieces[0], node);
    short mdData = pieces.length == 2 ? XMLUtils.parseNumber(node, pieces[1], Short.class) : 0;
    if (mdData != dmg && mdData != 0 && dmg != 0) {
      throw new InvalidXMLException(
          "Mismatching damage, parsed '" + text + ":" + dmg + "' but should be '" + text + ":"
              + mdData + "'",
          node);
    }
    short data = dmg != 0 ? dmg : mdData;

    if (data != 0 && !material.isLegacy() && !canUseMeta(material)) {
      throw new InvalidXMLException(
          "Material '" + material + "' cannot have a damage/meta value of " + data, node);
    }

    var md = Adapter.PGM_ITEM.visit(material, data);
    validateItem(md.getItemType(), node);
    return md;
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
    if (!material.isLegacy()) {
      var legacyMat = UNSAFE.toLegacy(material);
      if (legacyMat.isAir()) return new Material[] {material};
      material = legacyMat;
    }
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

  enum UpgradeStrat {
    MATERIAL,
    BLOCK_STATE,
    GOOD_LUCK;

    private static final UpgradeStrat[] VALUES = UpgradeStrat.values();

    int to(byte data) {
      return ordinal() << (data << 1);
    }

    static UpgradeStrat of(int value, byte data) {
      return VALUES[(value >> (data << 1)) & 0b11];
    }
  }

  public static UpgradeStrat getUpgradeStrategy(Material legacy, byte data) {
    if (!legacy.isLegacy()) return UpgradeStrat.MATERIAL;
    var id = legacy.getId();
    if (id < 0 || id >= 256) return UpgradeStrat.MATERIAL;
    return UpgradeStrat.of(UPGRADE_COUNT[id], data);
  }

  private static Material parseLegacyMaterial(String text, Node node) throws InvalidXMLException {
    int id = StringUtils.parseNumericId(text);
    if (id != -1) {
      var byId = BY_ID.get(id);
      if (byId == null)
        throw new InvalidXMLException("Could not find material with id '" + text + "'.", node);
      return byId;
    }
    text = text.toUpperCase(Locale.ROOT).replaceAll("\\s+", "_").replaceAll("\\W", "");

    // 1.21.9 introduced copper chains, so chain was renamed accordingly
    if (text.equals("CHAIN")) return Material.IRON_CHAIN;

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
    return pieces.length == 1
        ? adapter.visit(material)
        : adapter.visit(material, XMLUtils.parseNumber(node, pieces[1], Short.class));
  }

  private static boolean canUseMeta(Material material) {
    return material.getMaxDurability() > 0
        || material == Material.POTION
        || material == Material.SPLASH_POTION;
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
          default -> new ModernItemData(material, data);
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
