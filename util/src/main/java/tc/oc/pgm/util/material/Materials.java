package tc.oc.pgm.util.material;

import org.bukkit.Bukkit;
import org.bukkit.Effect;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Banner;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BannerMeta;
import org.bukkit.material.MaterialData;
import tc.oc.pgm.util.block.BlockFaces;

public interface Materials {

  static Material parseMaterial(String text) {
    // Since Bukkit changed SNOW_BALL to SNOWBALL, support both
    if (text.matches("(?)snow_?ball")) {
      text = text.contains("_") ? "snowball" : "snow_ball";
    }

    return Material.matchMaterial(text);
  }

  static boolean isWeapon(Material material) {
    if (material == null) return false;

    switch (material) {
        // Sword
      case WOOD_SWORD:
      case STONE_SWORD:
      case GOLD_SWORD:
      case IRON_SWORD:
      case DIAMOND_SWORD:
        // Axe
      case WOOD_AXE:
      case STONE_AXE:
      case GOLD_AXE:
      case IRON_AXE:
      case DIAMOND_AXE:
        // Pickaxe
      case WOOD_PICKAXE:
      case STONE_PICKAXE:
      case GOLD_PICKAXE:
      case IRON_PICKAXE:
      case DIAMOND_PICKAXE:
        // Spade
      case WOOD_SPADE:
      case STONE_SPADE:
      case GOLD_SPADE:
      case IRON_SPADE:
      case DIAMOND_SPADE:
        // Hoe
      case WOOD_HOE:
      case STONE_HOE:
      case GOLD_HOE:
      case IRON_HOE:
      case DIAMOND_HOE:
        // Others
      case BOW:
      case FLINT_AND_STEEL:
      case SHEARS:
      case STICK:
        return true;
      default:
        return false;
    }
  }

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
    return isSolid(block.getType());
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
    return isWater(block.getType());
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
    return isLava(block.getType());
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

  static boolean placeStanding(Location location, BannerMeta meta) {
    Block block = location.getBlock();
    block.setType(Material.STANDING_BANNER, false);

    final BlockState state = block.getState();
    if (state instanceof Banner) {
      Banner banner = (Banner) block.getState();
      applyToBlock(banner, meta);

      org.bukkit.material.Banner material = (org.bukkit.material.Banner) banner.getData();
      material.setFacingDirection(BlockFaces.yawToFace(location.getYaw()));
      banner.setData(material);
      banner.update(true, false);
      return true;
    }
    return false;
  }

  static Location getLocationWithYaw(Banner block) {
    Location location = block.getLocation();
    location.setYaw(
        BlockFaces.faceToYaw(((org.bukkit.material.Banner) block.getData()).getFacing()));
    return location;
  }

  static void playBreakEffect(Location location, MaterialData material) {
    location
        .getWorld()
        .playEffect(
            location, Effect.STEP_SOUND, material.getItemTypeId() + (material.getData() << 12));
  }
}
