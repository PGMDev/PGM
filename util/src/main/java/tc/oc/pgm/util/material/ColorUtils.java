package tc.oc.pgm.util.material;

import net.kyori.adventure.text.Component;
import org.bukkit.DyeColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Banner;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BannerMeta;
import org.bukkit.util.BlockVector;
import tc.oc.pgm.util.platform.Platform;

public interface ColorUtils {
  ColorUtils COLOR_UTILS = Platform.get(ColorUtils.class);

  boolean isColorAffected(Material material);

  void setColor(ItemStack item, DyeColor color);

  Material setColor(Material material, DyeColor color);

  void setColor(Block block, DyeColor color);

  boolean isColor(MaterialData data, DyeColor color);

  default void setColor(World world, Iterable<BlockVector> positions, DyeColor color) {
    for (BlockVector pos : positions) {
      setColor(world.getBlockAt(pos.getBlockX(), pos.getBlockY(), pos.getBlockZ()), color);
    }
  }

  BannerData createBanner(Banner banner);

  abstract class BannerData {
    protected final BannerMeta meta;

    public BannerData(BannerMeta meta) {
      this.meta = meta;
    }

    public abstract void setName(Component coloredName);

    public abstract DyeColor getBaseColor();

    public abstract BlockFace getFacing();

    public abstract ItemStack createItem();

    public abstract boolean placeStanding(Location location);
  }
}
