package tc.oc.material;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Banner;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BannerMeta;
import org.bukkit.material.MaterialData;
import tc.oc.block.BlockFaces;

public interface Materials {

  static boolean isSolid(Material material) {
    if (material == null) {
      return false;
    }

    switch (material) {
        // Bukkit considers these "solid" for some reason
      case SIGN_POST:
      case WALL_SIGN:
      case WOOD_PLATE:
      case STONE_PLATE:
      case IRON_PLATE:
      case GOLD_PLATE:
        return false;

      default:
        return material.isSolid();
    }
  }

  static boolean isSolid(MaterialData material) {
    return isSolid(material.getItemType());
  }

  static boolean isSolid(BlockState block) {
    return isSolid(block.getMaterial());
  }

  static boolean isWater(Material material) {
    return material == Material.WATER || material == Material.STATIONARY_WATER;
  }

  static boolean isWater(MaterialData material) {
    return isWater(material.getItemType());
  }

  static boolean isWater(Location location) {
    return isWater(location.getBlock().getType());
  }

  static boolean isWater(BlockState block) {
    return isWater(block.getMaterial());
  }

  static boolean isLava(Material material) {
    return material == Material.LAVA || material == Material.STATIONARY_LAVA;
  }

  static boolean isLava(MaterialData material) {
    return isLava(material.getItemType());
  }

  static boolean isLava(Location location) {
    return isLava(location.getBlock().getType());
  }

  static boolean isLava(BlockState block) {
    return isLava(block.getMaterial());
  }

  static boolean isLiquid(Material material) {
    return isWater(material) || isLava(material);
  }

  static boolean isClimbable(Material material) {
    return material == Material.LADDER || material == Material.VINE;
  }

  static boolean isClimbable(Location location) {
    return isClimbable(location.getBlock().getType());
  }

  static boolean isBucket(ItemStack bucket) {
    return isBucket(bucket.getType());
  }

  static boolean isBucket(Material bucket) {
    return bucket == Material.BUCKET
        || bucket == Material.LAVA_BUCKET
        || bucket == Material.WATER_BUCKET
        || bucket == Material.MILK_BUCKET;
  }

  static Material materialInBucket(ItemStack bucket) {
    return materialInBucket(bucket.getType());
  }

  static Material materialInBucket(Material bucket) {
    switch (bucket) {
      case BUCKET:
      case MILK_BUCKET:
        return Material.AIR;

      case LAVA_BUCKET:
        return Material.LAVA;
      case WATER_BUCKET:
        return Material.WATER;

      default:
        throw new IllegalArgumentException(bucket + " is not a bucket");
    }
  }

  static Material materialAt(Location location) {
    Block block = location.getBlock();
    return block == null ? Material.AIR : block.getType();
  }

  static BannerMeta getItemMeta(Banner block) {
    BannerMeta meta = (BannerMeta) Bukkit.getItemFactory().getItemMeta(Material.BANNER);
    meta.setBaseColor(block.getBaseColor());
    meta.setPatterns(block.getPatterns());
    return meta;
  }

  static void applyToBlock(Banner block, BannerMeta meta) {
    block.setBaseColor(meta.getBaseColor());
    block.setPatterns(meta.getPatterns());
  }

  static void placeStanding(Location location, BannerMeta meta) {
    Block block = location.getBlock();
    block.setType(Material.STANDING_BANNER);

    Banner banner = (Banner) block.getState();
    applyToBlock(banner, meta);

    org.bukkit.material.Banner material = (org.bukkit.material.Banner) banner.getData();
    material.setFacingDirection(BlockFaces.yawToFace(location.getYaw()));
    banner.setData(material);
    banner.update(true);
  }

  static Location getLocationWithYaw(Banner block) {
    Location location = block.getLocation();
    location.setYaw(
        BlockFaces.faceToYaw(((org.bukkit.material.Banner) block.getData()).getFacing()));
    return location;
  }
}
