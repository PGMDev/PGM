package tc.oc.pgm.platform.sportpaper.material;

import java.util.HashMap;
import java.util.Map;
import org.bukkit.Material;
import org.jspecify.annotations.Nullable;

class ModernMaterialNames {
  private static final Map<String, SpMaterialData> NAMES = new HashMap<>();

  // Data values for colored blocks are 0-15 in this order
  // Dye and banners swap black and white, so we account for that
  private static final String[] COLORS = {
    "WHITE", "ORANGE", "MAGENTA", "LIGHT_BLUE", "YELLOW", "LIME",
    "PINK", "GRAY", "LIGHT_GRAY", "CYAN", "PURPLE", "BLUE",
    "BROWN", "GREEN", "RED", "BLACK"
  };

  static {
    // Oak wood blocks
    put("OAK_LOG", 0, "LOG");
    put("OAK_WOOD", 0, "LOG");
    put("OAK_LEAVES", 0, "LEAVES");
    put("OAK_PLANKS", 0, "WOOD");
    put("OAK_SAPLING", 0, "SAPLING");
    put("OAK_SLAB", 0, "WOOD_STEP");
    put("OAK_STAIRS", 0, "WOOD_STAIRS");
    put("OAK_BOAT", 0, "BOAT");
    put("OAK_DOOR", 0, "WOOD_DOOR");
    put("OAK_FENCE", 0, "FENCE");
    put("OAK_FENCE_GATE", 0, "FENCE_GATE");
    put("OAK_BUTTON", 0, "WOOD_BUTTON");
    put("OAK_PRESSURE_PLATE", 0, "WOOD_PLATE");
    put("OAK_TRAPDOOR", 0, "TRAP_DOOR");
    put("OAK_SIGN", 0, "SIGN");
    put("OAK_WALL_SIGN", 0, "WALL_SIGN");

    // Spruce wood blocks
    put("SPRUCE_LOG", 1, "LOG");
    put("SPRUCE_WOOD", 1, "LOG");
    put("SPRUCE_LEAVES", 1, "LEAVES");
    put("SPRUCE_PLANKS", 1, "WOOD");
    put("SPRUCE_SAPLING", 1, "SAPLING");
    put("SPRUCE_SLAB", 1, "WOOD_STEP");
    put("SPRUCE_STAIRS", 0, "SPRUCE_WOOD_STAIRS");
    put("SPRUCE_DOOR", 0, "SPRUCE_DOOR_ITEM");
    put("SPRUCE_WALL_SIGN", 0, "WALL_SIGN");

    // Birch wood blocks
    put("BIRCH_LOG", 2, "LOG");
    put("BIRCH_WOOD", 2, "LOG");
    put("BIRCH_LEAVES", 2, "LEAVES");
    put("BIRCH_PLANKS", 2, "WOOD");
    put("BIRCH_SAPLING", 2, "SAPLING");
    put("BIRCH_SLAB", 2, "WOOD_STEP");
    put("BIRCH_STAIRS", 0, "BIRCH_WOOD_STAIRS");
    put("BIRCH_DOOR", 0, "BIRCH_DOOR_ITEM");
    put("BIRCH_WALL_SIGN", 0, "WALL_SIGN");

    // Jungle wood blocks
    put("JUNGLE_LOG", 3, "LOG");
    put("JUNGLE_WOOD", 3, "LOG");
    put("JUNGLE_LEAVES", 3, "LEAVES");
    put("JUNGLE_PLANKS", 3, "WOOD");
    put("JUNGLE_SAPLING", 3, "SAPLING");
    put("JUNGLE_SLAB", 3, "WOOD_STEP");
    put("JUNGLE_STAIRS", 0, "JUNGLE_WOOD_STAIRS");
    put("JUNGLE_DOOR", 0, "JUNGLE_DOOR_ITEM");
    put("JUNGLE_WALL_SIGN", 0, "WALL_SIGN");

    // Acacia wood blocks
    put("ACACIA_LOG", 0, "LOG_2");
    put("ACACIA_WOOD", 0, "LOG_2");
    put("ACACIA_LEAVES", 0, "LEAVES_2");
    put("ACACIA_PLANKS", 4, "WOOD");
    put("ACACIA_SAPLING", 4, "SAPLING");
    put("ACACIA_SLAB", 4, "WOOD_STEP");
    put("ACACIA_DOOR", 0, "ACACIA_DOOR_ITEM");
    put("ACACIA_WALL_SIGN", 0, "WALL_SIGN");

    // Dark oak wood blocks
    put("DARK_OAK_LOG", 1, "LOG_2");
    put("DARK_OAK_WOOD", 1, "LOG_2");
    put("DARK_OAK_LEAVES", 1, "LEAVES_2");
    put("DARK_OAK_PLANKS", 5, "WOOD");
    put("DARK_OAK_SAPLING", 5, "SAPLING");
    put("DARK_OAK_SLAB", 5, "WOOD_STEP");
    put("DARK_OAK_DOOR", 0, "DARK_OAK_DOOR_ITEM");
    put("DARK_OAK_WALL_SIGN", 0, "WALL_SIGN");

    // Stone variants
    put("GRANITE", 1, "STONE");
    put("POLISHED_GRANITE", 2, "STONE");
    put("DIORITE", 3, "STONE");
    put("POLISHED_DIORITE", 4, "STONE");
    put("ANDESITE", 5, "STONE");
    put("POLISHED_ANDESITE", 6, "STONE");
    put("STONE_BRICKS", 0, "SMOOTH_BRICK");
    put("MOSSY_STONE_BRICKS", 1, "SMOOTH_BRICK");
    put("CRACKED_STONE_BRICKS", 2, "SMOOTH_BRICK");
    put("CHISELED_STONE_BRICKS", 3, "SMOOTH_BRICK");
    put("STONE_BRICK_STAIRS", 0, "SMOOTH_STAIRS");
    put("STONE_BRICK_SLAB", 5, "STEP");

    // Dirt variants
    put("GRASS_BLOCK", 0, "GRASS");
    put("COARSE_DIRT", 1, "DIRT");
    put("PODZOL", 2, "DIRT");

    // Sand
    put("RED_SAND", 1, "SAND");

    // Sandstone
    put("CHISELED_SANDSTONE", 1, "SANDSTONE");
    put("SMOOTH_SANDSTONE", 2, "SANDSTONE");
    put("SANDSTONE_SLAB", 1, "STEP");
    put("CHISELED_RED_SANDSTONE", 1, "RED_SANDSTONE");
    put("RED_SANDSTONE_SLAB", 0, "STONE_SLAB2");

    // Slabs
    put("STONE_SLAB", 0, "STEP");
    put("COBBLESTONE_SLAB", 3, "STEP");
    put("BRICK_SLAB", 4, "STEP");
    put("NETHER_BRICK_SLAB", 6, "STEP");
    put("QUARTZ_SLAB", 7, "STEP");

    // Quartz
    put("CHISELED_QUARTZ_BLOCK", 1, "QUARTZ_BLOCK");
    put("QUARTZ_PILLAR", 2, "QUARTZ_BLOCK");

    // Prismarine
    put("PRISMARINE_BRICKS", 1, "PRISMARINE");
    put("DARK_PRISMARINE", 2, "PRISMARINE");

    // Nether bricks
    put("NETHER_BRICKS", 0, "NETHER_BRICK");
    put("NETHER_BRICK_FENCE", 0, "NETHER_FENCE");

    // Damaged anvils
    put("CHIPPED_ANVIL", 1, "ANVIL");
    put("DAMAGED_ANVIL", 2, "ANVIL");

    // Cobblestone walls
    put("COBBLESTONE_WALL", 0, "COBBLE_WALL");
    put("MOSSY_COBBLESTONE_WALL", 1, "COBBLE_WALL");

    // Sponge
    put("WET_SPONGE", 1, "SPONGE");

    // Infested blocks
    put("INFESTED_STONE", 0, "MONSTER_EGGS");
    put("INFESTED_COBBLESTONE", 1, "MONSTER_EGGS");
    put("INFESTED_STONE_BRICKS", 2, "MONSTER_EGGS");
    put("INFESTED_MOSSY_STONE_BRICKS", 3, "MONSTER_EGGS");
    put("INFESTED_CRACKED_STONE_BRICKS", 4, "MONSTER_EGGS");
    put("INFESTED_CHISELED_STONE_BRICKS", 5, "MONSTER_EGGS");

    // Flowers
    put("DANDELION", 0, "YELLOW_FLOWER");
    put("POPPY", 0, "RED_ROSE");
    put("BLUE_ORCHID", 1, "RED_ROSE");
    put("ALLIUM", 2, "RED_ROSE");
    put("AZURE_BLUET", 3, "RED_ROSE");
    put("RED_TULIP", 4, "RED_ROSE");
    put("ORANGE_TULIP", 5, "RED_ROSE");
    put("WHITE_TULIP", 6, "RED_ROSE");
    put("PINK_TULIP", 7, "RED_ROSE");
    put("OXEYE_DAISY", 8, "RED_ROSE");

    // Double-tall plants
    put("SUNFLOWER", 0, "DOUBLE_PLANT");
    put("LILAC", 1, "DOUBLE_PLANT");
    put("TALL_GRASS", 2, "DOUBLE_PLANT");
    put("LARGE_FERN", 3, "DOUBLE_PLANT");
    put("ROSE_BUSH", 4, "DOUBLE_PLANT");
    put("PEONY", 5, "DOUBLE_PLANT");

    // Short plants
    put("SHORT_GRASS", 1, "LONG_GRASS");
    put("FERN", 2, "LONG_GRASS");
    put("DEAD_BUSH", 0, "LONG_GRASS");

    // Stems
    put("ATTACHED_MELON_STEM", 7, "MELON_STEM");
    put("ATTACHED_PUMPKIN_STEM", 7, "PUMPKIN_STEM");

    // Fish
    put("COD", 0, "RAW_FISH");
    put("SALMON", 1, "RAW_FISH");
    put("TROPICAL_FISH", 2, "RAW_FISH");
    put("PUFFERFISH", 3, "RAW_FISH");
    put("COOKED_COD", 0, "COOKED_FISH");
    put("COOKED_SALMON", 1, "COOKED_FISH");

    // Skulls
    put("SKELETON_SKULL", 0, "SKULL_ITEM");
    put("SKELETON_WALL_SKULL", 0, "SKULL_ITEM");
    put("WITHER_SKELETON_SKULL", 1, "SKULL_ITEM");
    put("WITHER_SKELETON_WALL_SKULL", 1, "SKULL_ITEM");
    put("ZOMBIE_HEAD", 2, "SKULL_ITEM");
    put("ZOMBIE_WALL_HEAD", 2, "SKULL_ITEM");
    put("PLAYER_HEAD", 3, "SKULL_ITEM");
    put("PLAYER_WALL_HEAD", 3, "SKULL_ITEM");
    put("CREEPER_HEAD", 4, "SKULL_ITEM");
    put("CREEPER_WALL_HEAD", 4, "SKULL_ITEM");

    // Misc
    put("CHARCOAL", 1, "COAL");
    put("PETRIFIED_OAK_SLAB", 43, "WOOD_STEP");
    put("WOODEN_SLAB", 0, "WOOD_STEP");

    // Colored items
    for (int i = 0; i < COLORS.length; i++) {
      String c = COLORS[i];
      put(c + "_WOOL", i, "WOOL");
      put(c + "_CARPET", i, "CARPET");
      put(c + "_STAINED_GLASS", i, "STAINED_GLASS");
      put(c + "_STAINED_GLASS_PANE", i, "STAINED_GLASS_PANE");
      put(c + "_TERRACOTTA", i, "STAINED_CLAY");
      // Black and white swapped for the below items
      put(c + "_DYE", 15 - i, "INK_SACK");
      put(c + "_BANNER", 15 - i, "BANNER");
      put(c + "_WALL_BANNER", 15 - i, "WALL_BANNER");
    }
    put("INK_SAC", 0, "INK_SACK"); // Black dye
    put("COCOA_BEANS", 3, "INK_SACK"); // Brown dye
    put("LAPIS_LAZULI", 4, "INK_SACK"); // Blue dye
    put("BONE_MEAL", 15, "INK_SACK"); // White dye
    put("ROSE_RED", 1, "INK_SACK"); // Red dye pre-flattening, post 1.8
    put("CACTUS_GREEN", 2, "INK_SACK"); // Green dye pre-flattening, post 1.8
    put("DANDELION_YELLOW", 11, "INK_SACK"); // Yellow dye pre-flattening, post 1.8
    put("TERRACOTTA", 0, "HARD_CLAY");

    // Spawn eggs
    put("CREEPER_SPAWN_EGG", 50, "MONSTER_EGG");
    put("SKELETON_SPAWN_EGG", 51, "MONSTER_EGG");
    put("SPIDER_SPAWN_EGG", 52, "MONSTER_EGG");
    put("ZOMBIE_SPAWN_EGG", 54, "MONSTER_EGG");
    put("SLIME_SPAWN_EGG", 55, "MONSTER_EGG");
    put("GHAST_SPAWN_EGG", 56, "MONSTER_EGG");
    put("ZOMBIFIED_PIGLIN_SPAWN_EGG", 57, "MONSTER_EGG");
    put("ZOMBIE_PIGMAN_SPAWN_EGG", 57, "MONSTER_EGG");
    put("ENDERMAN_SPAWN_EGG", 58, "MONSTER_EGG");
    put("CAVE_SPIDER_SPAWN_EGG", 59, "MONSTER_EGG");
    put("SILVERFISH_SPAWN_EGG", 60, "MONSTER_EGG");
    put("BLAZE_SPAWN_EGG", 61, "MONSTER_EGG");
    put("MAGMA_CUBE_SPAWN_EGG", 62, "MONSTER_EGG");
    put("BAT_SPAWN_EGG", 65, "MONSTER_EGG");
    put("WITCH_SPAWN_EGG", 66, "MONSTER_EGG");
    put("ENDERMITE_SPAWN_EGG", 67, "MONSTER_EGG");
    put("GUARDIAN_SPAWN_EGG", 68, "MONSTER_EGG");
    put("SHULKER_SPAWN_EGG", 69, "MONSTER_EGG");
    put("PIG_SPAWN_EGG", 90, "MONSTER_EGG");
    put("SHEEP_SPAWN_EGG", 91, "MONSTER_EGG");
    put("COW_SPAWN_EGG", 92, "MONSTER_EGG");
    put("CHICKEN_SPAWN_EGG", 93, "MONSTER_EGG");
    put("SQUID_SPAWN_EGG", 94, "MONSTER_EGG");
    put("WOLF_SPAWN_EGG", 95, "MONSTER_EGG");
    put("MOOSHROOM_SPAWN_EGG", 96, "MONSTER_EGG");
    put("OCELOT_SPAWN_EGG", 98, "MONSTER_EGG");
    put("HORSE_SPAWN_EGG", 100, "MONSTER_EGG");
    put("RABBIT_SPAWN_EGG", 101, "MONSTER_EGG");
    put("VILLAGER_SPAWN_EGG", 120, "MONSTER_EGG");

    // Renamed items and blocks
    put("BEEF", 0, "RAW_BEEF");
    put("BRICK", 0, "CLAY_BRICK");
    put("BRICKS", 0, "BRICK");
    put("BROWN_MUSHROOM_BLOCK", 0, "HUGE_MUSHROOM_1");
    put("CAKE", 0, "CAKE_BLOCK");
    put("CARROT", 0, "CARROT_ITEM");
    put("CARROTS", 0, "CARROT");
    put("CARROT_ON_A_STICK", 0, "CARROT_STICK");
    put("CARVED_PUMPKIN", 0, "PUMPKIN");
    put("CAULDRON", 0, "CAULDRON_ITEM");
    put("CAVE_AIR", 0, "AIR");
    put("CHICKEN", 0, "RAW_CHICKEN");
    put("CLOCK", 0, "WATCH");
    put("COBWEB", 0, "WEB");
    put("COMMAND_BLOCK", 0, "COMMAND");
    put("COMMAND_BLOCK_MINECART", 0, "COMMAND_MINECART");
    put("COMPARATOR", 0, "REDSTONE_COMPARATOR");
    put("COOKED_PORKCHOP", 0, "GRILLED_PORK");
    put("CRAFTING_TABLE", 0, "WORKBENCH");
    put("DAYLIGHT_DETECTOR", 0, "DAYLIGHT_DETECTOR_INVERTED");
    put("DIAMOND_HORSE_ARMOR", 0, "DIAMOND_BARDING");
    put("DIAMOND_SHOVEL", 0, "DIAMOND_SPADE");
    put("ENCHANTING_TABLE", 0, "ENCHANTMENT_TABLE");
    put("ENCHANTED_GOLDEN_APPLE", 1, "GOLDEN_APPLE");
    put("ENDER_EYE", 0, "EYE_OF_ENDER");
    put("END_PORTAL", 0, "ENDER_PORTAL");
    put("END_PORTAL_FRAME", 0, "ENDER_PORTAL_FRAME");
    put("END_STONE", 0, "ENDER_STONE");
    put("EXPERIENCE_BOTTLE", 0, "EXP_BOTTLE");
    put("FARMLAND", 0, "SOIL");
    put("FILLED_MAP", 0, "MAP");
    put("FIREWORK_ROCKET", 0, "FIREWORK");
    put("FIREWORK_STAR", 0, "FIREWORK_CHARGE");
    put("FIRE_CHARGE", 0, "FIREBALL");
    put("FLOWER_POT", 0, "FLOWER_POT_ITEM");
    put("FURNACE", 0, "BURNING_FURNACE");
    put("FURNACE_MINECART", 0, "POWERED_MINECART");
    put("GLASS_PANE", 0, "THIN_GLASS");
    put("GLISTERING_MELON_SLICE", 0, "SPECKLED_MELON");
    put("GOLDEN_AXE", 0, "GOLD_AXE");
    put("GOLDEN_BOOTS", 0, "GOLD_BOOTS");
    put("GOLDEN_CHESTPLATE", 0, "GOLD_CHESTPLATE");
    put("GOLDEN_HELMET", 0, "GOLD_HELMET");
    put("GOLDEN_HOE", 0, "GOLD_HOE");
    put("GOLDEN_HORSE_ARMOR", 0, "GOLD_BARDING");
    put("GOLDEN_LEGGINGS", 0, "GOLD_LEGGINGS");
    put("GOLDEN_PICKAXE", 0, "GOLD_PICKAXE");
    put("GOLDEN_SHOVEL", 0, "GOLD_SPADE");
    put("GOLDEN_SWORD", 0, "GOLD_SWORD");
    put("GUNPOWDER", 0, "SULPHUR");
    put("HEAVY_WEIGHTED_PRESSURE_PLATE", 0, "IRON_PLATE");
    put("IRON_BARS", 0, "IRON_FENCE");
    put("IRON_DOOR", 0, "IRON_DOOR_BLOCK");
    put("IRON_HORSE_ARMOR", 0, "IRON_BARDING");
    put("IRON_SHOVEL", 0, "IRON_SPADE");
    put("LAVA", 0, "STATIONARY_LAVA");
    put("LEAD", 0, "LEASH");
    put("LIGHT_WEIGHTED_PRESSURE_PLATE", 0, "GOLD_PLATE");
    put("LILY_PAD", 0, "WATER_LILY");
    put("MAP", 0, "EMPTY_MAP");
    put("MELON", 0, "MELON_BLOCK");
    put("MELON_SLICE", 0, "MELON");
    put("MOVING_PISTON", 0, "PISTON_MOVING_PIECE");
    put("MUSHROOM_STEW", 0, "MUSHROOM_SOUP");
    put("MYCELIUM", 0, "MYCEL");
    put("NETHER_BRICK", 0, "NETHER_BRICK_ITEM");
    put("NETHER_PORTAL", 0, "PORTAL");
    put("NETHER_QUARTZ_ORE", 0, "QUARTZ_ORE");
    put("NETHER_WART", 0, "NETHER_WARTS");
    put("PISTON", 0, "PISTON_BASE");
    put("PISTON_HEAD", 0, "PISTON_EXTENSION");
    put("PORKCHOP", 0, "PORK");
    put("POTATO", 0, "POTATO_ITEM");
    put("POTATOES", 0, "POTATO");
    put("RAIL", 0, "RAILS");
    put("REDSTONE_LAMP", 0, "REDSTONE_LAMP_OFF");
    put("REDSTONE_ORE", 0, "GLOWING_REDSTONE_ORE");
    put("REDSTONE_TORCH", 0, "REDSTONE_TORCH_ON");
    put("RED_MUSHROOM_BLOCK", 0, "HUGE_MUSHROOM_2");
    put("REPEATER", 0, "DIODE");
    put("REPEATING_COMMAND_BLOCK", 0, "COMMAND");
    put("SNOWBALL", 0, "SNOW_BALL");
    put("SPAWNER", 0, "MOB_SPAWNER");
    put("STICKY_PISTON", 0, "PISTON_STICKY_BASE");
    put("STONE_PRESSURE_PLATE", 0, "STONE_PLATE");
    put("STONE_SHOVEL", 0, "STONE_SPADE");
    put("SUGAR_CANE", 0, "SUGAR_CANE_BLOCK");
    put("TNT_MINECART", 0, "EXPLOSIVE_MINECART");
    put("VOID_AIR", 0, "AIR");
    put("WALL_TORCH", 0, "TORCH");
    put("WATER", 0, "STATIONARY_WATER");
    put("WHEAT", 0, "CROPS");
    put("WHEAT_SEEDS", 0, "SEEDS");
  }

  private static void put(String modern, int data, String legacyName) {
    Material m = Material.getMaterial(legacyName);
    if (m != null) {
      NAMES.put(modern, new SpMaterialData(m, (short) data));
    }
  }

  static @Nullable SpMaterialData get(String normalized) {
    return NAMES.get(normalized);
  }
}
