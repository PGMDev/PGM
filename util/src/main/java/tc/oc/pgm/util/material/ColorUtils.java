package tc.oc.pgm.util.material;

import org.bukkit.ChatColor;
import org.bukkit.DyeColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Banner;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BannerMeta;
import org.bukkit.scoreboard.Team;
import org.bukkit.util.BlockVector;
import tc.oc.pgm.util.block.BlockFaces;
import tc.oc.pgm.util.platform.Platform;

public interface ColorUtils {
  ColorUtils COLOR_UTILS = Platform.requireInstance(ColorUtils.class);

  boolean isColorAffected(Material material);

  void setColor(ItemStack item, DyeColor color);

  Material setColor(Material material, DyeColor color);

  void setColor(Block block, DyeColor color);

  boolean isColor(BlockState block, DyeColor color);

  default void setColor(World world, Iterable<BlockVector> positions, DyeColor color) {
    for (BlockVector pos : positions) {
      setColor(world.getBlockAt(pos.getBlockX(), pos.getBlockY(), pos.getBlockZ()), color);
    }
  }

  default void setColor(Team team, ChatColor color) {}

  BannerData createBanner(Banner banner, String coloredName);

  abstract class BannerData {
    protected final BannerMeta meta;

    public BannerData(BannerMeta meta) {
      this.meta = meta;
    }

    public abstract DyeColor getBaseColor();

    public ItemStack createItem() {
      ItemStack is = new ItemStack(COLOR_UTILS.setColor(Materials.BANNER, getBaseColor()));
      COLOR_UTILS.setColor(is, getBaseColor());
      is.setItemMeta(meta);
      return is;
    }

    public boolean placeStanding(Location location) {
      Block block = location.getBlock();
      block.setType(COLOR_UTILS.setColor(Materials.STANDING_BANNER, getBaseColor()), false);
      COLOR_UTILS.setColor(block, getBaseColor());

      final BlockState state = block.getState();
      if (state instanceof Banner banner) {
        banner.setBaseColor(getBaseColor());
        banner.setPatterns(meta.getPatterns());

        org.bukkit.material.Banner material = (org.bukkit.material.Banner) banner.getData();
        material.setFacingDirection(BlockFaces.yawToFace(location.getYaw()));
        banner.setData(material);
        banner.update(true, false);
        return true;
      }
      return false;
    }
  }
}
