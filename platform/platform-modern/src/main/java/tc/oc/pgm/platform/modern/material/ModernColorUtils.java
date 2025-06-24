package tc.oc.pgm.platform.modern.material;

import static tc.oc.pgm.util.platform.Supports.Variant.PAPER;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.DyeColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Banner;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.block.data.Rotatable;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BannerMeta;
import tc.oc.pgm.util.block.BlockFaces;
import tc.oc.pgm.util.material.ColorUtils;
import tc.oc.pgm.util.material.MaterialData;
import tc.oc.pgm.util.platform.Supports;

@Supports(value = PAPER, minVersion = "1.20.6")
@SuppressWarnings("deprecation")
public class ModernColorUtils implements ColorUtils {

  private static final Map<Material, Map<DyeColor, Material>> COLORABLE_MATERIALS = new HashMap<>();

  static {
    registerColorable("WOOL", Material.LEGACY_WOOL);
    registerColorable("DYE", Material.LEGACY_INK_SACK);
    // 1.6
    registerColorable("CARPET", Material.LEGACY_CARPET);
    registerColorable("TERRACOTTA", Material.LEGACY_STAINED_CLAY);
    // 1.7
    registerColorable("STAINED_GLASS", Material.LEGACY_STAINED_GLASS);
    registerColorable("STAINED_GLASS_PANE", Material.LEGACY_STAINED_GLASS_PANE);

    // 1.8
    registerColorable("BANNER", Material.LEGACY_BANNER, Material.LEGACY_STANDING_BANNER);
    registerColorable("WALL_BANNER", Material.LEGACY_WALL_BANNER);

    // 1.11
    registerColorable("SHULKER_BOX", getLegacyColored("SHULKER_BOX"));

    // 1.12
    registerColorable("CONCRETE", Material.LEGACY_CONCRETE);
    registerColorable("CONCRETE_POWDER", Material.LEGACY_CONCRETE_POWDER);
    registerColorable("GLAZED_TERRACOTTA", getLegacyColored("GLAZED_TERRACOTTA"));

    // 1.17
    registerColorable("CANDLE");
    registerColorable("CANDLE_CAKE");

    // TODO: PLATFORM 1.20 maybe automate the finding of existing colored materials
  }

  static void registerColorable(String suffix, Material... legacy) {
    EnumMap<DyeColor, Material> colorToModern = new EnumMap<>(DyeColor.class);
    for (DyeColor color : DyeColor.values()) {
      Material modern = Material.getMaterial(color.name() + "_" + suffix);
      if (modern == null) {
        throw new IllegalStateException("Unknown material " + color.name() + ": " + suffix);
      }
      colorToModern.put(color, modern);
      COLORABLE_MATERIALS.put(modern, colorToModern);
    }
    for (Material mat : legacy) {
      COLORABLE_MATERIALS.put(mat, colorToModern);
    }
  }

  static Material[] getLegacyColored(String suffix) {
    DyeColor[] colors = DyeColor.values();
    var mats = new Material[colors.length];
    for (int i = 0; i < colors.length; i++) {
      mats[i] = Material.getMaterial("LEGACY_" + colors[i].name() + "_" + suffix);
    }
    return mats;
  }

  @Override
  public boolean isColorAffected(Material material) {
    return COLORABLE_MATERIALS.containsKey(material);
  }

  @Override
  public void setColor(ItemStack item, DyeColor color) {
    item.setType(setColor(item.getType(), color));
  }

  @Override
  public Material setColor(Material material, DyeColor color) {
    var mappings = COLORABLE_MATERIALS.get(material);
    return mappings != null ? mappings.get(color) : material;
  }

  @Override
  public void setColor(Block block, DyeColor color) {
    block.setType(setColor(block.getType(), color));
  }

  @Override
  public boolean isColor(MaterialData data, DyeColor color) {
    return setColor(data.getItemType(), color) == data.getItemType();
  }

  @Override
  public BannerData createBanner(Banner block) {
    DyeColor baseColor = block.getBaseColor();
    Material baseMaterial = setColor(Material.LEGACY_BANNER, block.getBaseColor());
    BlockFace facing = ((Rotatable) block.getBlockData()).getRotation();

    BannerMeta meta = (BannerMeta) Bukkit.getItemFactory().getItemMeta(block.getType());
    meta.setPatterns(block.getPatterns());

    return new BannerData(meta) {
      @Override
      public void setName(Component coloredName) {
        meta.displayName(coloredName);
      }

      @Override
      public DyeColor getBaseColor() {
        return baseColor;
      }

      @Override
      public BlockFace getFacing() {
        return facing;
      }

      public ItemStack createItem() {
        ItemStack is = new ItemStack(baseMaterial);
        is.setItemMeta(meta);
        return is;
      }

      public boolean placeStanding(Location location) {
        Block block = location.getBlock();
        block.setType(baseMaterial, false);

        final BlockState state = block.getState();
        if (state instanceof Banner banner) {
          banner.setBaseColor(baseColor);
          banner.setPatterns(meta.getPatterns());

          Rotatable material = (Rotatable) banner.getBlockData();
          material.setRotation(BlockFaces.yawToFace(location.getYaw()));
          banner.setBlockData(material);
          banner.update(true, false);
          return true;
        }
        return false;
      }
    };
  }
}
