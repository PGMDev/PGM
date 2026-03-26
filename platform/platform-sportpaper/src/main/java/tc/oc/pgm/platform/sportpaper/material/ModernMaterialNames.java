package tc.oc.pgm.platform.sportpaper.material;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import org.bukkit.Material;
import org.jspecify.annotations.Nullable;

class ModernMaterialNames {

  protected record MaterialMapping(
      @Nullable Material itemMaterial,
      @Nullable Short itemDamage,
      @Nullable Material blockMaterial,
      @Nullable Short blockDamage) {

    protected MaterialMapping {
      if (itemMaterial != null && !SpMaterialUtils.isItem(itemMaterial)) {
        itemMaterial = null;
        itemDamage = null;
      }

      if (blockMaterial != null && !blockMaterial.isBlock()) {
        blockMaterial = null;
        blockDamage = null;
      }
    }

    MaterialMapping(@Nullable Material material, @Nullable Short damage) {
      this(material, damage, material, damage);
    }

    boolean single() {
      if (itemMaterial == null || blockMaterial == null) return true;
      return itemMaterial == blockMaterial && Objects.equals(itemDamage, blockDamage);
    }

    Material type() {
      return itemMaterial != null ? itemMaterial : blockMaterial;
    }

    Optional<Short> data() {
      return Optional.ofNullable(itemMaterial != null ? itemDamage : blockDamage);
    }

    @Nullable
    Material itemType() {
      return itemMaterial;
    }

    Optional<Short> itemData() {
      return Optional.ofNullable(itemDamage);
    }

    @Nullable
    Material blockType() {
      return blockMaterial;
    }

    Optional<Short> blockData() {
      return Optional.ofNullable(blockDamage);
    }
  }

  private static final Map<String, MaterialMapping> NAMES = new HashMap<>();

  // Data values for colored blocks are 0-15 in this order
  // Dye and banners swap black and white, so we account for that
  private static final String[] COLORS = {
    "WHITE", "ORANGE", "MAGENTA", "LIGHT_BLUE", "YELLOW", "LIME",
    "PINK", "GRAY", "LIGHT_GRAY", "CYAN", "PURPLE", "BLUE",
    "BROWN", "GREEN", "RED", "BLACK"
  };

  // Data values for wood types are 0-5 in this order
  private static final String[] WOODS = {"OAK", "SPRUCE", "BIRCH", "JUNGLE", "ACACIA", "DARK_OAK"};

  static {
    // Colored items
    for (int i = 0; i < COLORS.length; i++) {
      String color = COLORS[i];
      put(color + "_WOOL", "WOOL", i);
      put(color + "_CARPET", "CARPET", i);
      put(color + "_STAINED_GLASS", "STAINED_GLASS", i);
      put(color + "_STAINED_GLASS_PANE", "STAINED_GLASS_PANE", i);
      put(color + "_TERRACOTTA", "STAINED_CLAY", i);
      // Black and white swapped for the below items
      put(color + "_DYE", "INK_SACK", 15 - i);
      put(color + "_BANNER", "BANNER", 15 - i);
      put(color + "_WALL_BANNER", "WALL_BANNER", 15 - i);
    }

    put("INK_SAC", "INK_SACK:0"); // Black dye
    put("COCOA_BEANS", "INK_SACK:3"); // Brown dye
    put("LAPIS_LAZULI", "INK_SACK:4"); // Blue dye
    put("BONE_MEAL", "INK_SACK:15"); // White dye
    put("ROSE_RED", "INK_SACK:1"); // Red dye pre-flattening, post 1.8
    put("CACTUS_GREEN", "INK_SACK:2"); // Green dye pre-flattening, post 1.8
    put("DANDELION_YELLOW", "INK_SACK:11"); // Yellow dye pre-flattening, post 1.8

    // Wood types
    for (int i = 0; i < WOODS.length; i++) {
      var type = WOODS[i];

      var suffix = i >= 4 ? "_2" : "";
      put(type + "_LOG", "LOG" + suffix, i % 4);
      put(type + "_WOOD", "LOG" + suffix, 12 + (i % 4));
      put(type + "_LEAVES", "LEAVES" + suffix, i % 4);

      put(type + "_PLANKS", "WOOD", i);
      put(type + "_SAPLING", "SAPLING", i);
      put(type + "_SLAB", "WOOD_STEP", i);

      // Map all below to oak, as per-wood variants did not exist in legacy
      put(type + "_BOAT", "BOAT");
      put(type + "_BUTTON", "WOOD_BUTTON");
      put(type + "_PRESSURE_PLATE", "WOOD_PLATE");
      put(type + "_TRAPDOOR", "TRAP_DOOR");
      put(type + "_SIGN", "SIGN", "SIGN_POST");
      put(type + "_WALL_SIGN", "WALL_SIGN");

      // Exclude oak as we handle it separately
      if (i > 0) {
        // Spruce, birch, and jungle all use _WOOD_STAIRS
        // Dark oak and acacia are already flattened in legacy
        if (i < 4) put(type + "_STAIRS", type + "_WOOD_STAIRS");
        put(type + "_DOOR", type + "_DOOR_ITEM", type + "_DOOR");
      }
    }

    // Map oak to generic legacy name
    put("OAK_STAIRS", "WOOD_STAIRS");
    put("OAK_DOOR", "WOOD_DOOR", "WOODEN_DOOR");
    put("OAK_FENCE", "FENCE");
    put("OAK_FENCE_GATE", "FENCE_GATE");

    // Weapons and tools
    put("WOODEN_AXE", "WOOD_AXE");
    put("WOODEN_HOE", "WOOD_HOE");
    put("WOODEN_PICKAXE", "WOOD_PICKAXE");
    put("WOODEN_SHOVEL", "WOOD_SPADE");
    put("WOODEN_SWORD", "WOOD_SWORD");
    put("GOLDEN_AXE", "GOLD_AXE");
    put("GOLDEN_HOE", "GOLD_HOE");
    put("GOLDEN_PICKAXE", "GOLD_PICKAXE");
    put("GOLDEN_SHOVEL", "GOLD_SPADE");
    put("GOLDEN_SWORD", "GOLD_SWORD");
    put("STONE_SHOVEL", "STONE_SPADE");
    put("IRON_SHOVEL", "IRON_SPADE");
    put("DIAMOND_SHOVEL", "DIAMOND_SPADE");

    // Armor
    put("GOLDEN_HELMET", "GOLD_HELMET");
    put("GOLDEN_CHESTPLATE", "GOLD_CHESTPLATE");
    put("GOLDEN_LEGGINGS", "GOLD_LEGGINGS");
    put("GOLDEN_BOOTS", "GOLD_BOOTS");
    put("GOLDEN_HORSE_ARMOR", "GOLD_BARDING");
    put("IRON_HORSE_ARMOR", "IRON_BARDING");
    put("DIAMOND_HORSE_ARMOR", "DIAMOND_BARDING");

    // Stone variants
    put("GRANITE", "STONE:1");
    put("POLISHED_GRANITE", "STONE:2");
    put("DIORITE", "STONE:3");
    put("POLISHED_DIORITE", "STONE:4");
    put("ANDESITE", "STONE:5");
    put("POLISHED_ANDESITE", "STONE:6");
    put("STONE_BRICKS", "SMOOTH_BRICK:0");
    put("MOSSY_STONE_BRICKS", "SMOOTH_BRICK:1");
    put("CRACKED_STONE_BRICKS", "SMOOTH_BRICK:2");
    put("CHISELED_STONE_BRICKS", "SMOOTH_BRICK:3");
    put("STONE_BRICK_STAIRS", "SMOOTH_STAIRS");
    put("STONE_BRICK_SLAB", "STEP:5");

    // Dirt variants
    put("GRASS_BLOCK", "GRASS");
    put("COARSE_DIRT", "DIRT:1");
    put("PODZOL", "DIRT:2");

    // Sand
    put("RED_SAND", "SAND:1");

    // Sandstone
    put("CHISELED_SANDSTONE", "SANDSTONE:1");
    put("CUT_SANDSTONE", "SANDSTONE:2");
    put("SMOOTH_SANDSTONE", "SANDSTONE:2");
    put("CHISELED_RED_SANDSTONE", "RED_SANDSTONE:1");
    put("CUT_RED_SANDSTONE", "RED_SANDSTONE:2");
    put("SMOOTH_RED_SANDSTONE", "RED_SANDSTONE:2");

    // Slabs
    put("STONE_SLAB", "STEP:0");
    put("SMOOTH_STONE_SLAB", "STEP:0");
    put("SANDSTONE_SLAB", "STEP:1");
    put("PETRIFIED_OAK_SLAB", "STEP:2");
    put("COBBLESTONE_SLAB", "STEP:3");
    put("BRICK_SLAB", "STEP:4");
    put("NETHER_BRICK_SLAB", "STEP:6");
    put("QUARTZ_SLAB", "STEP:7");
    put("RED_SANDSTONE_SLAB", "STONE_SLAB2:0");

    // Modern-only slabs that reasonably remap backwards
    put("SMOOTH_SANDSTONE_SLAB", "STEP:1"); // remap to sandstone slab
    put("CUT_SANDSTONE_SLAB", "STEP:1"); // remap to sandstone slab
    put("SMOOTH_RED_SANDSTONE_SLAB", "STONE_SLAB2:0"); // remap to red sandstone slab
    put("CUT_RED_SANDSTONE_SLAB", "STONE_SLAB2:0"); // remap to red sandstone slab

    // Quartz
    put("CHISELED_QUARTZ_BLOCK", "QUARTZ_BLOCK:1");
    put("QUARTZ_PILLAR", "QUARTZ_BLOCK:2");

    // Prismarine
    put("PRISMARINE_BRICKS", "PRISMARINE:1");
    put("DARK_PRISMARINE", "PRISMARINE:2");

    // Nether bricks
    put("NETHER_BRICKS", "NETHER_BRICK");
    put("NETHER_BRICK_FENCE", "NETHER_FENCE");

    // Damaged anvils
    put("CHIPPED_ANVIL", "ANVIL:1");
    put("DAMAGED_ANVIL", "ANVIL:2");

    // Cobblestone walls
    put("COBBLESTONE_WALL", "COBBLE_WALL");
    put("MOSSY_COBBLESTONE_WALL", "COBBLE_WALL:1");

    // Sponge
    put("WET_SPONGE", "SPONGE:1");

    // Infested blocks
    put("INFESTED_STONE", "MONSTER_EGGS:0");
    put("INFESTED_COBBLESTONE", "MONSTER_EGGS:1");
    put("INFESTED_STONE_BRICKS", "MONSTER_EGGS:2");
    put("INFESTED_MOSSY_STONE_BRICKS", "MONSTER_EGGS:3");
    put("INFESTED_CRACKED_STONE_BRICKS", "MONSTER_EGGS:4");
    put("INFESTED_CHISELED_STONE_BRICKS", "MONSTER_EGGS:5");

    // Flowers
    put("DANDELION", "YELLOW_FLOWER");
    put("POPPY", "RED_ROSE:0");
    put("BLUE_ORCHID", "RED_ROSE:1");
    put("ALLIUM", "RED_ROSE:2");
    put("AZURE_BLUET", "RED_ROSE:3");
    put("RED_TULIP", "RED_ROSE:4");
    put("ORANGE_TULIP", "RED_ROSE:5");
    put("WHITE_TULIP", "RED_ROSE:6");
    put("PINK_TULIP", "RED_ROSE:7");
    put("OXEYE_DAISY", "RED_ROSE:8");

    // Double-tall plants
    put("SUNFLOWER", "DOUBLE_PLANT:0");
    put("LILAC", "DOUBLE_PLANT:1");
    put("TALL_GRASS", "DOUBLE_PLANT:2");
    put("LARGE_FERN", "DOUBLE_PLANT:3");
    put("ROSE_BUSH", "DOUBLE_PLANT:4");
    put("PEONY", "DOUBLE_PLANT:5");

    // Short plants
    put("DEAD_BUSH", "LONG_GRASS:0");
    put("SHORT_GRASS", "LONG_GRASS:1");
    put("FERN", "LONG_GRASS:2");

    // Stems
    put("ATTACHED_MELON_STEM", "MELON_STEM:7");
    put("ATTACHED_PUMPKIN_STEM", "PUMPKIN_STEM:7");

    // Fish
    put("COD", "RAW_FISH:0");
    put("SALMON", "RAW_FISH:1");
    put("TROPICAL_FISH", "RAW_FISH:2");
    put("PUFFERFISH", "RAW_FISH:3");
    put("COOKED_COD", "COOKED_FISH");
    put("COOKED_SALMON", "COOKED_FISH:1");

    // Skulls
    put("SKELETON_SKULL", "SKULL_ITEM:0", "SKULL");
    put("SKELETON_WALL_SKULL", "SKULL_ITEM:0", "SKULL");
    put("WITHER_SKELETON_SKULL", "SKULL_ITEM:1", "SKULL");
    put("WITHER_SKELETON_WALL_SKULL", "SKULL_ITEM:1", "SKULL");
    put("ZOMBIE_HEAD", "SKULL_ITEM:2", "SKULL");
    put("ZOMBIE_WALL_HEAD", "SKULL_ITEM:2", "SKULL");
    put("PLAYER_HEAD", "SKULL_ITEM:3", "SKULL");
    put("PLAYER_WALL_HEAD", "SKULL_ITEM:3", "SKULL");
    put("CREEPER_HEAD", "SKULL_ITEM:4", "SKULL");
    put("CREEPER_WALL_HEAD", "SKULL_ITEM:4", "SKULL");

    // Spawn eggs
    put("CREEPER_SPAWN_EGG", "MONSTER_EGG:50");
    put("SKELETON_SPAWN_EGG", "MONSTER_EGG:51");
    put("SPIDER_SPAWN_EGG", "MONSTER_EGG:52");
    put("ZOMBIE_SPAWN_EGG", "MONSTER_EGG:54");
    put("SLIME_SPAWN_EGG", "MONSTER_EGG:55");
    put("GHAST_SPAWN_EGG", "MONSTER_EGG:56");
    put("ZOMBIFIED_PIGLIN_SPAWN_EGG", "MONSTER_EGG:57");
    put("ZOMBIE_PIGMAN_SPAWN_EGG", "MONSTER_EGG:57");
    put("ENDERMAN_SPAWN_EGG", "MONSTER_EGG:58");
    put("CAVE_SPIDER_SPAWN_EGG", "MONSTER_EGG:59");
    put("SILVERFISH_SPAWN_EGG", "MONSTER_EGG:60");
    put("BLAZE_SPAWN_EGG", "MONSTER_EGG:61");
    put("MAGMA_CUBE_SPAWN_EGG", "MONSTER_EGG:62");
    put("BAT_SPAWN_EGG", "MONSTER_EGG:65");
    put("WITCH_SPAWN_EGG", "MONSTER_EGG:66");
    put("ENDERMITE_SPAWN_EGG", "MONSTER_EGG:67");
    put("GUARDIAN_SPAWN_EGG", "MONSTER_EGG:68");
    put("SHULKER_SPAWN_EGG", "MONSTER_EGG:69");
    put("PIG_SPAWN_EGG", "MONSTER_EGG:90");
    put("SHEEP_SPAWN_EGG", "MONSTER_EGG:91");
    put("COW_SPAWN_EGG", "MONSTER_EGG:92");
    put("CHICKEN_SPAWN_EGG", "MONSTER_EGG:93");
    put("SQUID_SPAWN_EGG", "MONSTER_EGG:94");
    put("WOLF_SPAWN_EGG", "MONSTER_EGG:95");
    put("MOOSHROOM_SPAWN_EGG", "MONSTER_EGG:96");
    put("OCELOT_SPAWN_EGG", "MONSTER_EGG:98");
    put("HORSE_SPAWN_EGG", "MONSTER_EGG:100");
    put("RABBIT_SPAWN_EGG", "MONSTER_EGG:101");
    put("VILLAGER_SPAWN_EGG", "MONSTER_EGG:120");

    // Misc
    put("TERRACOTTA", "HARD_CLAY");
    put("CHARCOAL", "COAL:1");
    put("BEEF", "RAW_BEEF");
    put("BREWING_STAND", "BREWING_STAND_ITEM", "BREWING_STAND");
    put("BRICKS", "BRICK");
    put("BROWN_MUSHROOM_BLOCK", "HUGE_MUSHROOM_1");
    put("CAKE", "CAKE", "CAKE_BLOCK");
    put("CARROT", "CARROT_ITEM", "CARROT");
    put("CARROTS", "CARROT");
    put("CARROT_ON_A_STICK", "CARROT_STICK");
    put("CARVED_PUMPKIN", "PUMPKIN");
    put("CAULDRON", "CAULDRON_ITEM", "CAULDRON");
    put("CAVE_AIR", "AIR");
    put("CHEST_MINECART", "STORAGE_MINECART");
    put("CHICKEN", "RAW_CHICKEN");
    put("CLOCK", "WATCH");
    put("COBWEB", "WEB");
    put("COMMAND_BLOCK", "COMMAND");
    put("COMMAND_BLOCK_MINECART", "COMMAND_MINECART");
    put("COMPARATOR", "REDSTONE_COMPARATOR");
    put("COOKED_PORKCHOP", "GRILLED_PORK");
    put("CRAFTING_TABLE", "WORKBENCH");
    put("ENCHANTING_TABLE", "ENCHANTMENT_TABLE");
    put("ENCHANTED_GOLDEN_APPLE", "GOLDEN_APPLE:1");
    put("ENDER_EYE", "EYE_OF_ENDER");
    put("END_PORTAL", "ENDER_PORTAL");
    put("END_PORTAL_FRAME", "ENDER_PORTAL_FRAME");
    put("END_STONE", "ENDER_STONE");
    put("EXPERIENCE_BOTTLE", "EXP_BOTTLE");
    put("FARMLAND", "SOIL");
    put("FILLED_MAP", "MAP");
    put("FIREWORK_ROCKET", "FIREWORK");
    put("FIREWORK_STAR", "FIREWORK_CHARGE");
    put("FIRE_CHARGE", "FIREBALL");
    put("FLOWER_POT", "FLOWER_POT_ITEM", "FLOWER_POT");
    put("FURNACE_MINECART", "POWERED_MINECART");
    put("GLASS_PANE", "THIN_GLASS");
    put("GLISTERING_MELON_SLICE", "SPECKLED_MELON");
    put("GUNPOWDER", "SULPHUR");
    put("HEAVY_WEIGHTED_PRESSURE_PLATE", "IRON_PLATE");
    put("IRON_BARS", "IRON_FENCE");
    put("IRON_DOOR", "IRON_DOOR", "IRON_DOOR_BLOCK");
    put("LAVA", "STATIONARY_LAVA");
    put("LEAD", "LEASH");
    put("LIGHT_WEIGHTED_PRESSURE_PLATE", "GOLD_PLATE");
    put("LILY_PAD", "WATER_LILY");
    put("MELON_SLICE", "MELON");
    put("MOVING_PISTON", "PISTON_MOVING_PIECE");
    put("MUSHROOM_STEW", "MUSHROOM_SOUP");
    put("MYCELIUM", "MYCEL");
    put("NETHER_PORTAL", "PORTAL");
    put("NETHER_QUARTZ_ORE", "QUARTZ_ORE");
    put("NETHER_WART", "NETHER_WARTS", "NETHER_STALK");
    put("PISTON", "PISTON_BASE");
    put("PISTON_HEAD", "PISTON_EXTENSION");
    put("PORKCHOP", "PORK");
    put("POTATO", "POTATO_ITEM", "POTATO");
    put("POTATOES", "POTATO");
    put("RAIL", "RAILS");
    put("REDSTONE_LAMP", "REDSTONE_LAMP_OFF");
    put("REDSTONE_TORCH", "REDSTONE_TORCH_ON");
    put("RED_MUSHROOM_BLOCK", "HUGE_MUSHROOM_2");
    put("REPEATER", "DIODE");
    put("REPEATING_COMMAND_BLOCK", "COMMAND");
    put("SNOWBALL", "SNOW_BALL");
    put("SPAWNER", "MOB_SPAWNER");
    put("STICKY_PISTON", "PISTON_STICKY_BASE");
    put("STONE_PRESSURE_PLATE", "STONE_PLATE");
    put("SUGAR_CANE", "SUGAR_CANE", "SUGAR_CANE_BLOCK");
    put("TNT_MINECART", "EXPLOSIVE_MINECART");
    put("VOID_AIR", "AIR");
    put("WALL_TORCH", "TORCH");
    put("WATER", "STATIONARY_WATER");
    put("WHEAT", "WHEAT", "CROPS");
    put("WHEAT_SEEDS", "SEEDS");
    put("WRITABLE_BOOK", "BOOK_AND_QUILL");

    // Music discs
    put("MUSIC_DISC_13", "GOLD_RECORD");
    put("MUSIC_DISC_CAT", "GREEN_RECORD");
    put("MUSIC_DISC_BLOCKS", "RECORD_3");
    put("MUSIC_DISC_CHIRP", "RECORD_4");
    put("MUSIC_DISC_FAR", "RECORD_5");
    put("MUSIC_DISC_MALL", "RECORD_6");
    put("MUSIC_DISC_MELLOHI", "RECORD_7");
    put("MUSIC_DISC_STAL", "RECORD_8");
    put("MUSIC_DISC_STRAD", "RECORD_9");
    put("MUSIC_DISC_WARD", "RECORD_10");
    put("MUSIC_DISC_11", "RECORD_11");
    put("MUSIC_DISC_WAIT", "RECORD_12");
  }

  private static Material parseMaterial(String name) {
    int split = name.indexOf(':', 2);
    String materialName = split == -1 ? name : name.substring(0, split);

    Material material = Material.getMaterial(materialName);
    if (material == null) throw new IllegalArgumentException("Unknown material: " + materialName);

    return material;
  }

  private static @Nullable Short parseDamage(String name) {
    int split = name.indexOf(':', 2);
    if (split == -1) return null;

    return Short.parseShort(name.substring(split + 1));
  }

  private static void put(String modern, String legacy) {
    NAMES.put(modern, new MaterialMapping(parseMaterial(legacy), parseDamage(legacy)));
  }

  private static void put(String modern, String legacy, int data) {
    Material material = Material.getMaterial(legacy);
    if (material == null) throw new IllegalArgumentException("Unknown material: " + legacy);

    NAMES.put(modern, new MaterialMapping(material, (short) data));
  }

  private static void put(String modern, String itemLegacy, String blockLegacy) {
    NAMES.put(
        modern,
        new MaterialMapping(
            parseMaterial(itemLegacy), parseDamage(itemLegacy),
            parseMaterial(blockLegacy), parseDamage(blockLegacy)));
  }

  static @Nullable MaterialMapping get(String name) {
    return NAMES.get(name);
  }
}
