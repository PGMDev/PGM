package tc.oc.pgm.util.material;

import com.cryptomorin.xseries.XMaterial;
import com.cryptomorin.xseries.particles.ParticleDisplay;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import org.bukkit.DyeColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.block.Banner;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.Rotatable;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BannerMeta;
import tc.oc.pgm.util.block.BlockFaces;

public interface Materials {

  Map<String, Material> MATERIAL_CACHE = new HashMap<>();

  static Material parseMaterial(String text) {
    Material cachedMaterial = MATERIAL_CACHE.get(text);
    if (cachedMaterial != null) {
      return cachedMaterial;
    }

    Optional<XMaterial> xMaterial = XMaterial.matchXMaterial(text);

    if (!xMaterial.isPresent()) {
      Material type = LegacyMaterials.parseLegacyMaterial(text);
      if (type != null) {
        MATERIAL_CACHE.put(text, type);
        return type;
      }
    }

    Material result = null;

    if (xMaterial.isPresent()) {
      result = xMaterial.get().parseMaterial();

      MATERIAL_CACHE.put(text, result);
    }

    return result;
  }

  static void colorBlock(Block block, DyeColor color) {
    Material material = colorMaterial(block.getType(), color);
    if (material != null) {
      block.setType(material);
    }
  }

  static Material colorMaterial(Material material, DyeColor dyeColor) {
    String type = material.name();
    int index = type.indexOf('_');
    if (index == -1) return null;

    String realType = type.substring(index);
    return Material.getMaterial(dyeColor.name() + realType);
  }

  static boolean isSolid(Material material) {
    if (material == null) {
      return false;
    }

    return material.isSolid();
  }

  static boolean isSolid(BlockState block) {
    return isSolid(block.getType());
  }

  static boolean isWater(Material material) {
    return material == Material.WATER;
  }

  static boolean isWater(Location location) {
    return isWater(location.getBlock().getType());
  }

  static boolean isLava(Material material) {
    return material == Material.LAVA;
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
    return material == Material.LADDER || material == Material.VINE || material == Material.CHAIN;
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

  static void placeStanding(Location location, BannerMeta meta, Material bannerType) {
    Block block = location.getBlock();
    block.setType(bannerType);

    Banner banner = (Banner) block.getState();
    banner.setPatterns(meta.getPatterns());

    Rotatable blockData = (Rotatable) banner.getBlockData();
    blockData.setRotation(BlockFaces.yawToFace(location.getYaw()));
    banner.setBlockData(blockData);
    banner.update(true);
  }

  static void playBreakEffect(Location location, BlockData blockData) {
    ParticleDisplay.of(Particle.BLOCK_CRACK).withBlock(blockData).spawn(location);
  }
}
