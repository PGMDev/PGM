package tc.oc.pgm.util.material;

import static org.bukkit.Material.*;
import static tc.oc.pgm.util.bukkit.BukkitUtils.parse;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import java.util.Map;
import java.util.Set;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.BlockState;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import tc.oc.pgm.util.bukkit.BukkitUtils;

public interface Materials {
  Map<Integer, Material> BY_ID =
      new ImmutableMap.Builder<Integer, Material>() {
        {
          for (Material value : Material.values()) {
            put(value.getId(), value);
          }
        }
      }.build();

  // Weapons
  Material WOODEN_SWORD = parse("WOOD_SWORD", "WOODEN_SWORD");
  Material GOLDEN_SWORD = parse("GOLD_SWORD", "GOLDEN_SWORD");
  Material WOOD_AXE = parse("WOOD_AXE", "WOODEN_AXE");
  Material GOLD_AXE = parse("GOLD_AXE", "GOLDEN_AXE");
  Material WOOD_PICKAXE = parse("WOOD_PICKAXE", "WOODEN_PICKAXE");
  Material GOLD_PICKAXE = parse("GOLD_PICKAXE", "GOLDEN_PICKAXE");
  Material WOOD_SHOVEL = parse("WOOD_SPADE", "WOODEN_SHOVEL");
  Material STONE_SHOVEL = parse("STONE_SPADE", "STONE_SHOVEL");
  Material GOLD_SHOVEL = parse("GOLD_SPADE", "GOLDEN_SHOVEL");
  Material IRON_SHOVEL = parse("IRON_SPADE", "IRON_SHOVEL");
  Material DIAMOND_SHOVEL = parse("DIAMOND_SPADE", "DIAMOND_SHOVEL");
  Material WOOD_HOE = parse("WOOD_HOE", "WOODEN_HOE");
  Material GOLD_HOE = parse("GOLD_HOE", "GOLDEN_HOE");

  Material SIGN_POST = parse("SIGN_POST", "LEGACY_SIGN_POST");
  Material WALL_SIGN = parse("WALL_SIGN", "LEGACY_WALL_SIGN");
  Material WOOD_PLATE = parse("WOOD_PLATE", "LEGACY_WOOD_PLATE");
  Material STONE_PLATE = parse("STONE_PLATE", "STONE_PRESSURE_PLATE");
  Material IRON_PLATE = parse("IRON_PLATE", "HEAVY_WEIGHTED_PRESSURE_PLATE");
  Material GOLD_PLATE = parse("GOLD_PLATE", "LIGHT_WEIGHTED_PRESSURE_PLATE");

  Material STILL_WATER = parse("STATIONARY_WATER", "LEGACY_STATIONARY_WATER");
  Material STILL_LAVA = parse("STATIONARY_LAVA", "LEGACY_STATIONARY_LAVA");

  Material BANNER = parse("BANNER", "LEGACY_BANNER");
  Material STANDING_BANNER = parse("STANDING_BANNER", "LEGACY_STANDING_BANNER");
  Material WOOL = parse("WOOL", "LEGACY_WOOL");
  Material CARPET = parse("CARPET", "LEGACY_CARPET");
  Material STAINED_CLAY = parse("STAINED_CLAY", "LEGACY_STAINED_CLAY");
  Material STAINED_GLASS = parse("STAINED_GLASS", "LEGACY_STAINED_GLASS");
  Material STAINED_GLASS_PANE = parse("STAINED_GLASS_PANE", "LEGACY_STAINED_GLASS_PANE");
  Material SHORT_GRASS = parse("LONG_GRASS", "SHORT_GRASS");

  Material WEB = parse("WEB", "COBWEB");
  Material LEASH = parse("LEASH", "LEAD");
  Material LILY_PAD = parse("WATER_LILY", "LILY_PAD");
  Material SIGN = parse("SIGN", "OAK_SIGN");
  Material SKULL = parse("SKULL_ITEM", "SKELETON_SKULL");
  Material PLAYER_HEAD = parse("SKULL_ITEM", "PLAYER_HEAD");
  Material WOOD_DOOR = parse("WOOD_DOOR", "OAK_DOOR");
  Material BOOK_AND_QUILL = parse("BOOK_AND_QUILL", "WRITABLE_BOOK");
  Material EYE_OF_ENDER = parse("EYE_OF_ENDER", "ENDER_EYE");
  Material FIREWORK = parse("FIREWORK", "FIREWORK_ROCKET");
  Material WATCH = parse("WATCH", "CLOCK");
  Material DYE = parse("INK_SACK", "LEGACY_INK_SACK");
  Material IRON_DOOR = parse("IRON_DOOR_BLOCK", "IRON_DOOR");
  Material RAW_FISH = parse("RAW_FISH", "COD");
  Material WORKBENCH = parse("WORKBENCH", "CRAFTING_TABLE");
  Material SOIL = parse("SOIL", "FARMLAND");
  Material MOVING_PISTON = parse("PISTON_MOVING_PIECE", "MOVING_PISTON");
  Material PISTON_HEAD = parse("PISTON_EXTENSION", "PISTON_HEAD");

  static Material parse(String... names) {
    return BukkitUtils.parse(Material::valueOf, names);
  }

  Set<Material> WEAPONS =
      ImmutableSet.of(
          WOODEN_SWORD,
          STONE_SWORD,
          GOLDEN_SWORD,
          IRON_SWORD,
          DIAMOND_SWORD,
          WOOD_AXE,
          STONE_AXE,
          GOLD_AXE,
          IRON_AXE,
          DIAMOND_AXE,
          WOOD_PICKAXE,
          STONE_PICKAXE,
          GOLD_PICKAXE,
          IRON_PICKAXE,
          DIAMOND_PICKAXE,
          WOOD_SHOVEL,
          STONE_SHOVEL,
          GOLD_SHOVEL,
          IRON_SHOVEL,
          DIAMOND_SHOVEL,
          WOOD_HOE,
          STONE_HOE,
          GOLD_HOE,
          IRON_HOE,
          DIAMOND_HOE,
          BOW,
          FLINT_AND_STEEL,
          SHEARS,
          STICK);

  Set<Material> SOLID_EXCLUSIONS =
      ImmutableSet.of(SIGN_POST, WALL_SIGN, WOOD_PLATE, STONE_PLATE, IRON_PLATE, GOLD_PLATE);

  static Material parseMaterial(String text) {
    // Since Bukkit changed SNOW_BALL to SNOWBALL, support both
    if (text.matches("(?)snow_?ball")) {
      text = text.contains("_") ? "snowball" : "snow_ball";
    }

    return Material.matchMaterial(text);
  }

  static boolean isWeapon(Material material) {
    if (material == null) return false;
    return WEAPONS.contains(material);
  }

  static boolean isSolid(Material material) {
    if (material == null) {
      return false;
    }
    return material.isSolid()
        && !SOLID_EXCLUSIONS.contains(material)
        && !material.name().endsWith("PRESSURE_PLATE");
  }

  static boolean itemsSimilar(
      ItemStack first, ItemStack second, boolean skipDur, boolean skipCheckingName) {
    if (first == second) {
      return true;
    }
    if (second == null
        || first == null
        || !first.getType().equals(second.getType())
        || (!skipDur && first.getDurability() != second.getDurability())) {
      return false;
    }
    final boolean hasMeta1 = first.hasItemMeta();
    final boolean hasMeta2 = second.hasItemMeta();
    if (!hasMeta1 && !hasMeta2) {
      return true;
    }

    final ItemMeta meta1 = hasMeta1 ? first.getItemMeta() : null;
    final ItemMeta meta2 = hasMeta2 ? second.getItemMeta() : null;

    final String prevName1 = meta1 != null ? meta1.getDisplayName() : null;
    final String prevName2 = meta2 != null ? meta2.getDisplayName() : null;
    if (skipCheckingName) {
      if (meta1 != null) {
        meta1.setDisplayName(null);
      }
      if (meta2 != null) {
        meta2.setDisplayName(null);
      }
    }

    try {
      return Bukkit.getItemFactory().equals(meta1, meta2);
    } finally {
      if (skipCheckingName) {
        if (meta1 != null) {
          meta1.setDisplayName(prevName1);
        }
        if (meta2 != null) {
          meta2.setDisplayName(prevName2);
        }
      }
    }
  }

  static boolean isSolid(MaterialData material) {
    return isSolid(material.getItemType());
  }

  static boolean isSolid(BlockState block) {
    return isSolid(block.getType());
  }

  static boolean isWater(Material material) {
    return material == Material.WATER || material == STILL_WATER;
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
    return material == Material.LAVA || material == STILL_LAVA;
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
}
