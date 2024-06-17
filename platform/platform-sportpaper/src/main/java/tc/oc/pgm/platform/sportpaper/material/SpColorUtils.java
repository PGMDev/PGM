package tc.oc.pgm.platform.sportpaper.material;

import static org.bukkit.Material.BANNER;
import static tc.oc.pgm.util.platform.Supports.Variant.SPORTPAPER;

import com.google.common.collect.ImmutableSet;
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
import org.bukkit.material.MaterialData;
import org.bukkit.material.Wool;
import org.bukkit.util.BlockVector;
import tc.oc.pgm.util.block.BlockFaces;
import tc.oc.pgm.util.material.ColorUtils;
import tc.oc.pgm.util.platform.Supports;
import tc.oc.pgm.util.text.TextTranslations;

@Supports(SPORTPAPER)
@SuppressWarnings("deprecation")
public class SpColorUtils implements ColorUtils {

  public static final ImmutableSet<Material> COLOR_AFFECTED = ImmutableSet.of(
      Material.INK_SACK,
      Material.WOOL,
      Material.CARPET,
      Material.STAINED_CLAY,
      Material.STAINED_GLASS,
      Material.STAINED_GLASS_PANE,
      BANNER);

  @Override
  public boolean isColorAffected(Material material) {
    return COLOR_AFFECTED.contains(material);
  }

  @Override
  public void setColor(ItemStack item, DyeColor color) {
    Material type = item.getType();
    if (type == Material.WOOL) {
      item.setData(new Wool(color));
    } else if (type == Material.INK_SACK) {
      item.setData(new Dye(color));
    } else {
      item.setData(new MaterialData(item.getType(), color.getWoolData()));
    }
    item.setDurability(color.getWoolData());
  }

  @Override
  public Material setColor(Material material, DyeColor color) {
    // This is a no-op because material never changes due to color in 1.8
    return material;
  }

  @Override
  public void setColor(Block block, DyeColor color) {
    block.setData(color.getWoolData());
  }

  @Override
  public boolean isColor(BlockState block, DyeColor color) {
    return color.getWoolData() == block.getRawData();
  }

  public void setColor(World world, Iterable<BlockVector> positions, DyeColor color) {
    byte blockData = color.getWoolData();
    for (BlockVector pos : positions) {
      world.getBlockAt(pos.getBlockX(), pos.getBlockY(), pos.getBlockZ()).setData(blockData);
    }
  }

  @Override
  public BannerData createBanner(Banner block) {
    BannerMeta meta = (BannerMeta) Bukkit.getItemFactory().getItemMeta(BANNER);
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

          org.bukkit.material.Banner material = (org.bukkit.material.Banner) banner.getData();
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
