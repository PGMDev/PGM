package tc.oc.pgm.platform.sportpaper.material;

import static org.bukkit.Material.BANNER;
import static tc.oc.pgm.util.platform.Supports.Variant.SPORTPAPER;

import com.google.common.collect.ImmutableSet;
import org.bukkit.Bukkit;
import org.bukkit.DyeColor;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Banner;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BannerMeta;
import org.bukkit.material.Dye;
import org.bukkit.material.MaterialData;
import org.bukkit.material.Wool;
import org.bukkit.util.BlockVector;
import tc.oc.pgm.util.material.ColorUtils;
import tc.oc.pgm.util.platform.Supports;

@Supports(SPORTPAPER)
@SuppressWarnings("deprecation")
public class SpColorUtils implements ColorUtils {

  public static final ImmutableSet<Material> COLOR_AFFECTED =
      ImmutableSet.of(
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
  public void setColor(Block block, DyeColor color) {
    block.setData(color.getWoolData());
  }

  @Override
  public DyeColor getColor(BlockState block) {
    return DyeColor.getByWoolData(block.getRawData());
  }

  public void setColor(World world, Iterable<BlockVector> positions, DyeColor color) {
    byte blockData = color.getWoolData();
    for (BlockVector pos : positions) {
      world.getBlockAt(pos.getBlockX(), pos.getBlockY(), pos.getBlockZ()).setData(blockData);
    }
  }

  @Override
  public BannerData createBanner(Banner block, String coloredName) {
    BannerMeta meta = (BannerMeta) Bukkit.getItemFactory().getItemMeta(BANNER);
    meta.setBaseColor(block.getBaseColor());
    meta.setPatterns(block.getPatterns());
    meta.setDisplayName(coloredName);

    return new BannerData(meta) {
      @Override
      public DyeColor getBaseColor() {
        return meta.getBaseColor();
      }
    };
  }
}
