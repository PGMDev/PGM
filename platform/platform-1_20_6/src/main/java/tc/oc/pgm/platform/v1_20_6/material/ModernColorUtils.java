package tc.oc.pgm.platform.v1_20_6.material;

import static tc.oc.pgm.util.platform.Supports.Variant.PAPER;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.DyeColor;
import org.bukkit.Material;
import org.bukkit.block.Banner;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BannerMeta;
import org.bukkit.scoreboard.Team;
import tc.oc.pgm.util.material.ColorUtils;
import tc.oc.pgm.util.platform.Supports;

@Supports(value = PAPER, minVersion = "1.20.6")
@SuppressWarnings("deprecation")
public class ModernColorUtils implements ColorUtils {

  private static final Map<Material, Map<DyeColor, Material>> COLORABLE_MATERIALS = new HashMap<>();

  static {
    registerColorable("WOOL");
    registerColorable("DYE");
    registerColorable("CARPET");
    registerColorable("STAINED_CLAY");
    registerColorable("STAINED_GLASS");
    registerColorable("BANNER");
    registerColorable("WALL_BANNER");
    registerColorable("STANDING_BANNER");
    // TODO: find others
  }

  static void registerColorable(String suffix) {
    EnumMap<DyeColor, Material> map = new EnumMap<>(DyeColor.class);
    for (DyeColor color : DyeColor.values()) {
      Material m = Material.getMaterial(color.name() + "_" + suffix);
      map.put(color, m);
      COLORABLE_MATERIALS.put(m, map);
    }
    COLORABLE_MATERIALS.put(Material.getMaterial(suffix, true), map);
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
  public boolean isColor(BlockState block, DyeColor color) {
    return setColor(block.getType(), color) == block.getType();
  }

  @Override
  public void setColor(Team team, ChatColor color) {
    team.setColor(color);
  }

  @Override
  public BannerData createBanner(Banner block, String coloredName) {
    DyeColor baseColor = block.getBaseColor();
    BannerMeta meta = (BannerMeta) Bukkit.getItemFactory().getItemMeta(block.getType());
    meta.setPatterns(block.getPatterns());
    meta.setDisplayName(coloredName);

    return new BannerData(meta) {
      @Override
      public DyeColor getBaseColor() {
        return baseColor;
      }
    };
  }
}
