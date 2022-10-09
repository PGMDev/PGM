package tc.oc.pgm.util.bukkit;

import com.google.common.collect.ImmutableMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import org.bukkit.*;
import org.bukkit.inventory.Recipe;
import org.bukkit.plugin.Plugin;
import org.bukkit.potion.PotionEffectType;

public interface BukkitUtils {

  AtomicReference<Plugin> PLUGIN = new AtomicReference<>();

  @Deprecated
  static Plugin getPlugin() {
    // HACK: Sometimes util code needs to access a Plugin,
    // we assume it to be PGM for now, but there should be a formal
    // way of registering this in the future.
    if (PLUGIN.get() == null) {
      PLUGIN.set(Bukkit.getPluginManager().getPlugin("PGM"));
    }
    return PLUGIN.get();
  }

  Boolean isSportPaper = Bukkit.getServer().getVersion().contains("SportPaper");

  static boolean isSportPaper() {
    return isSportPaper;
  }

  static void addRecipe(World world, Recipe recipe) {
    if (BukkitUtils.isSportPaper()) {
      world.addRecipe(recipe);
    } else {
      Bukkit.addRecipe(recipe);
    }
  }

  static void resetRecipes(World world) {
    if (BukkitUtils.isSportPaper()) {
      world.resetRecipes();
    } else {
      Bukkit.resetRecipes();
    }
  }

  /** Makes strings have pretty colors */
  static String colorize(String s) {
    return ChatColor.translateAlternateColorCodes(
        '&', ChatColor.translateAlternateColorCodes('`', s));
  }

  static ChatColor dyeColorToChatColor(DyeColor dyeColor) {
    ChatColor chatColor = DYE_CHAT_MAP.get(dyeColor);
    if (chatColor != null) {
      return chatColor;
    } else {
      return ChatColor.WHITE;
    }
  }

  static DyeColor chatColorToDyeColor(ChatColor chatColor) {
    DyeColor dyeColor = CHAT_DYE_MAP.get(chatColor);
    if (dyeColor != null) {
      return dyeColor;
    } else {
      return DyeColor.WHITE;
    }
  }

  Map<DyeColor, ChatColor> DYE_CHAT_MAP =
      ImmutableMap.<DyeColor, ChatColor>builder()
          .put(DyeColor.BLACK, ChatColor.BLACK)
          .put(DyeColor.BLUE, ChatColor.BLUE) // DARK_BLUE always looks ugly in chat
          .put(DyeColor.GREEN, ChatColor.DARK_GREEN)
          .put(DyeColor.CYAN, ChatColor.DARK_AQUA)
          .put(DyeColor.RED, ChatColor.DARK_RED)
          .put(DyeColor.PURPLE, ChatColor.DARK_PURPLE)
          .put(DyeColor.ORANGE, ChatColor.GOLD)
          .put(DyeColor.SILVER, ChatColor.GRAY)
          .put(DyeColor.GRAY, ChatColor.DARK_GRAY)
          .put(DyeColor.LIGHT_BLUE, ChatColor.BLUE)
          .put(DyeColor.LIME, ChatColor.GREEN)
          .put(DyeColor.BROWN, ChatColor.DARK_RED)
          .put(DyeColor.MAGENTA, ChatColor.LIGHT_PURPLE)
          .put(DyeColor.YELLOW, ChatColor.YELLOW)
          .put(DyeColor.WHITE, ChatColor.WHITE)
          .put(DyeColor.PINK, ChatColor.RED)
          .build();

  Map<ChatColor, DyeColor> CHAT_DYE_MAP =
      ImmutableMap.<ChatColor, DyeColor>builder()
          .put(ChatColor.AQUA, DyeColor.LIGHT_BLUE)
          .put(ChatColor.BLACK, DyeColor.BLACK)
          .put(ChatColor.BLUE, DyeColor.BLUE)
          .put(ChatColor.DARK_AQUA, DyeColor.CYAN)
          .put(ChatColor.DARK_BLUE, DyeColor.BLUE)
          .put(ChatColor.DARK_GRAY, DyeColor.GRAY)
          .put(ChatColor.DARK_GREEN, DyeColor.GREEN)
          .put(ChatColor.DARK_PURPLE, DyeColor.PURPLE)
          .put(ChatColor.DARK_RED, DyeColor.RED)
          .put(ChatColor.GOLD, DyeColor.ORANGE)
          .put(ChatColor.GRAY, DyeColor.SILVER)
          .put(ChatColor.GREEN, DyeColor.LIME)
          .put(ChatColor.LIGHT_PURPLE, DyeColor.MAGENTA)
          .put(ChatColor.RED, DyeColor.RED)
          .put(ChatColor.WHITE, DyeColor.WHITE)
          .put(ChatColor.YELLOW, DyeColor.YELLOW)
          .build();

  static Color colorOf(ChatColor chatColor) {
    Color color = CHAT_COLOR_MAP.get(chatColor);
    if (color != null) {
      return color;
    } else {
      return Color.WHITE;
    }
  }

  Map<ChatColor, Color> CHAT_COLOR_MAP =
      ImmutableMap.<ChatColor, Color>builder()
          .put(ChatColor.BLACK, Color.fromRGB(0, 0, 0))
          .put(ChatColor.DARK_BLUE, Color.fromRGB(0, 0, 170))
          .put(ChatColor.DARK_GREEN, Color.fromRGB(0, 170, 0))
          .put(ChatColor.DARK_AQUA, Color.fromRGB(0, 170, 170))
          .put(ChatColor.DARK_RED, Color.fromRGB(170, 0, 0))
          .put(ChatColor.DARK_PURPLE, Color.fromRGB(170, 0, 170))
          .put(ChatColor.GOLD, Color.fromRGB(255, 170, 0))
          .put(ChatColor.GRAY, Color.fromRGB(170, 170, 170))
          .put(ChatColor.DARK_GRAY, Color.fromRGB(85, 85, 85))
          .put(ChatColor.BLUE, Color.fromRGB(85, 85, 255))
          .put(ChatColor.GREEN, Color.fromRGB(85, 255, 85))
          .put(ChatColor.AQUA, Color.fromRGB(85, 255, 255))
          .put(ChatColor.RED, Color.fromRGB(255, 85, 85))
          .put(ChatColor.LIGHT_PURPLE, Color.fromRGB(255, 85, 255))
          .put(ChatColor.YELLOW, Color.fromRGB(255, 255, 85))
          .put(ChatColor.WHITE, Color.fromRGB(255, 255, 255))
          .build();

  static String potionEffectTypeName(final PotionEffectType type) {
    String name = POTION_EFFECT_MAP.get(type);
    if (name != null) {
      return name;
    } else {
      return "Unknown";
    }
  }

  Map<PotionEffectType, String> POTION_EFFECT_MAP =
      ImmutableMap.<PotionEffectType, String>builder()
          .put(PotionEffectType.BLINDNESS, "Blindness")
          .put(PotionEffectType.CONFUSION, "Nausea")
          .put(PotionEffectType.DAMAGE_RESISTANCE, "Resistance")
          .put(PotionEffectType.FAST_DIGGING, "Haste")
          .put(PotionEffectType.FIRE_RESISTANCE, "Fire Resistance")
          .put(PotionEffectType.HARM, "Instant Damage")
          .put(PotionEffectType.HEAL, "Instant Health")
          .put(PotionEffectType.HUNGER, "Hunger")
          .put(PotionEffectType.INCREASE_DAMAGE, "Strength")
          .put(PotionEffectType.INVISIBILITY, "Invisibility")
          .put(PotionEffectType.JUMP, "Jump Boost")
          .put(PotionEffectType.NIGHT_VISION, "Night Vision")
          .put(PotionEffectType.POISON, "Poison")
          .put(PotionEffectType.REGENERATION, "Regeneration")
          .put(PotionEffectType.SLOW, "Slowness")
          .put(PotionEffectType.SLOW_DIGGING, "Mining Fatigue")
          .put(PotionEffectType.SPEED, "Speed")
          .put(PotionEffectType.WATER_BREATHING, "Water Breathing")
          .put(PotionEffectType.WEAKNESS, "Weakness")
          .put(PotionEffectType.WITHER, "Wither")
          .put(PotionEffectType.HEALTH_BOOST, "Health Boost")
          .put(PotionEffectType.ABSORPTION, "Absorption")
          .put(PotionEffectType.SATURATION, "Saturation")
          .build();
}
