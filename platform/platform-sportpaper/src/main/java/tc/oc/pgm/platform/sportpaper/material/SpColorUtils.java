package tc.oc.pgm.platform.sportpaper.material;

import static tc.oc.pgm.util.platform.Supports.Variant.SPORTPAPER;

import java.util.EnumMap;
import java.util.Map;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.DyeColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Banner;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BannerMeta;
import org.bukkit.material.Dye;
import org.bukkit.material.Wool;
import org.bukkit.util.BlockVector;
import tc.oc.pgm.util.block.BlockFaces;
import tc.oc.pgm.util.material.ColorUtils;
import tc.oc.pgm.util.material.MaterialData;
import tc.oc.pgm.util.platform.Supports;
import tc.oc.pgm.util.text.TextTranslations;

@Supports(SPORTPAPER)
@SuppressWarnings("deprecation")
public class SpColorUtils implements ColorUtils {

  private static final Map<Material, Material> COLORABLE_MATERIALS = new EnumMap<>(Material.class);

  static {
    registerColorable(Material.INK_SACK);
    registerColorable(Material.WOOL);
    registerColorable(Material.CARPET);
    registerColorable(Material.HARD_CLAY, Material.STAINED_CLAY);
    registerColorable(Material.STAINED_CLAY);
    registerColorable(Material.STAINED_GLASS);
    registerColorable(Material.STAINED_GLASS_PANE);
    registerColorable(Material.BANNER);
  }

  private static void registerColorable(Material from, Material to) {
    COLORABLE_MATERIALS.put(from, to);
  }

  private static void registerColorable(Material material) {
    registerColorable(material, material);
  }

  @Override
  public boolean isColorAffected(Material material) {
    return COLORABLE_MATERIALS.containsKey(material);
  }

  @Override
  public void setColor(ItemStack item, DyeColor color) {
    Material type = setColor(item.getType(), color);
    item.setType(type);
    if (type == Material.WOOL) {
      item.setData(new Wool(color));
    } else if (type == Material.INK_SACK) {
      item.setData(new Dye(color));
    } else {
      item.setData(
          type.getNewData(type == Material.BANNER ? color.getDyeData() : color.getWoolData()));
    }
    item.setDurability(item.getData().getData());
  }

  @Override
  public Material setColor(Material material, DyeColor color) {
    // Return the material or its mapped colorable material,
    // but this is otherwise a no-op since the actual material
    // never changes due to color in legacy.
    return COLORABLE_MATERIALS.getOrDefault(material, material);
  }

  @Override
  public void setColor(Block block, DyeColor color) {
    block.setType(setColor(block.getType(), color));
    block.setData(color.getWoolData());
  }

  @Override
  public boolean isColor(MaterialData data, DyeColor color) {
    return data instanceof LegacyMaterialData legacyData
        && color.getWoolData() == legacyData.getData();
  }

  public void setColor(World world, Iterable<BlockVector> positions, DyeColor color) {
    byte blockData = color.getWoolData();
    for (BlockVector pos : positions) {
      world.getBlockAt(pos.getBlockX(), pos.getBlockY(), pos.getBlockZ()).setData(blockData);
    }
  }

  @Override
  public BannerData createBanner(Banner block) {
    BannerMeta meta = (BannerMeta) Bukkit.getItemFactory().getItemMeta(Material.BANNER);
    meta.setBaseColor(block.getBaseColor());
    meta.setPatterns(block.getPatterns());
    BlockFace facing = ((org.bukkit.material.Banner) block.getData()).getFacing();

    return new BannerData(meta) {
      @Override
      public void setName(Component coloredName) {
        meta.setDisplayName(TextTranslations.translateLegacy(coloredName));
      }

      @Override
      public DyeColor getBaseColor() {
        return meta.getBaseColor();
      }

      @Override
      public BlockFace getFacing() {
        return facing;
      }

      public ItemStack createItem() {
        ItemStack is = new ItemStack(Material.BANNER);
        is.setItemMeta(meta);
        return is;
      }

      public boolean placeStanding(Location location) {
        Block block = location.getBlock();
        block.setType(Material.STANDING_BANNER, false);

        final BlockState state = block.getState();
        if (state instanceof Banner) {
          Banner banner = (Banner) block.getState();
          banner.setBaseColor(meta.getBaseColor());
          banner.setPatterns(meta.getPatterns());

          var material = (org.bukkit.material.Banner) banner.getData();
          material.setFacingDirection(BlockFaces.yawToFace(location.getYaw()));
          banner.setData(material);
          banner.update(true, false);
          return true;
        }
        return false;
      }
    };
  }
}
