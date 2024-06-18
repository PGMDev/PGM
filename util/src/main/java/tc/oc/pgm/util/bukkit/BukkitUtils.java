package tc.oc.pgm.util.bukkit;

import com.google.common.collect.ImmutableMap;
import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import org.bukkit.*;
import org.bukkit.plugin.Plugin;
import org.bukkit.potion.PotionEffectType;

public interface BukkitUtils {

  AtomicReference<Plugin> PLUGIN = new AtomicReference<>();

  static Plugin getPlugin() {
    // HACK: Sometimes util code needs to access a Plugin,
    // we assume it to be PGM for now, but there should be a formal
    // way of registering this in the future.
    if (PLUGIN.get() == null) {
      PLUGIN.set(Bukkit.getPluginManager().getPlugin("PGM"));
    }
    return PLUGIN.get();
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

  Map<DyeColor, ChatColor> DYE_CHAT_MAP = ImmutableMap.<DyeColor, ChatColor>builder()
      .put(DyeColor.BLACK, ChatColor.BLACK)
      .put(DyeColor.BLUE, ChatColor.BLUE) // DARK_BLUE always looks ugly in chat
      .put(DyeColor.GREEN, ChatColor.DARK_GREEN)
      .put(DyeColor.CYAN, ChatColor.DARK_AQUA)
      .put(DyeColor.RED, ChatColor.DARK_RED)
      .put(DyeColor.PURPLE, ChatColor.DARK_PURPLE)
      .put(DyeColor.ORANGE, ChatColor.GOLD)
      .put(DyeColors.SILVER, ChatColor.GRAY)
      .put(DyeColor.GRAY, ChatColor.DARK_GRAY)
      .put(DyeColor.LIGHT_BLUE, ChatColor.BLUE)
      .put(DyeColor.LIME, ChatColor.GREEN)
      .put(DyeColor.BROWN, ChatColor.DARK_RED)
      .put(DyeColor.MAGENTA, ChatColor.LIGHT_PURPLE)
      .put(DyeColor.YELLOW, ChatColor.YELLOW)
      .put(DyeColor.WHITE, ChatColor.WHITE)
      .put(DyeColor.PINK, ChatColor.RED)
      .build();

  Map<ChatColor, DyeColor> CHAT_DYE_MAP = ImmutableMap.<ChatColor, DyeColor>builder()
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
      .put(ChatColor.GRAY, DyeColors.SILVER)
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

  Map<ChatColor, Color> CHAT_COLOR_MAP = ImmutableMap.<ChatColor, Color>builder()
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

  Map<PotionEffectType, String> POTION_EFFECT_MAP = ImmutableMap.<PotionEffectType, String>builder()
      .put(PotionEffects.BLINDNESS, "Blindness")
      .put(PotionEffects.NAUSEA, "Nausea")
      .put(PotionEffects.RESISTANCE, "Resistance")
      .put(PotionEffects.HASTE, "Haste")
      .put(PotionEffects.FIRE_RESISTANCE, "Fire Resistance")
      .put(PotionEffects.INSTANT_DAMAGE, "Instant Damage")
      .put(PotionEffects.INSTANT_HEALTH, "Instant Health")
      .put(PotionEffects.HUNGER, "Hunger")
      .put(PotionEffects.STRENGTH, "Strength")
      .put(PotionEffects.INVISIBILITY, "Invisibility")
      .put(PotionEffects.JUMP_BOOST, "Jump Boost")
      .put(PotionEffects.NIGHT_VISION, "Night Vision")
      .put(PotionEffects.POISON, "Poison")
      .put(PotionEffects.REGENERATION, "Regeneration")
      .put(PotionEffects.SLOWNESS, "Slowness")
      .put(PotionEffects.MINING_FATIGUE, "Mining Fatigue")
      .put(PotionEffects.SPEED, "Speed")
      .put(PotionEffects.WATER_BREATHING, "Water Breathing")
      .put(PotionEffects.WEAKNESS, "Weakness")
      .put(PotionEffects.WITHER, "Wither")
      .put(PotionEffects.HEALTH_BOOST, "Health Boost")
      .put(PotionEffects.ABSORPTION, "Absorption")
      .put(PotionEffects.SATURATION, "Saturation")
      .build();

  static <T> T parse(Function<String, T> parser, String... names) {
    for (String name : names) {
      try {
        T result = parser.apply(name);
        if (result != null) return result;
      } catch (Exception ignore) {
      }
    }
    throw new IllegalArgumentException(
        "Name not found while parsing one of " + Arrays.toString(names));
  }
}
