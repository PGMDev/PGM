package tc.oc.pgm.util.bukkit;

import java.util.HashMap;
import java.util.Map;
import org.bukkit.enchantments.Enchantment;
import tc.oc.pgm.util.StringUtils;

public class Enchantments {
  private static final Map<String, Enchantment> byName = new HashMap<>();

  static {
    for (Enchantment value : Enchantment.values()) {
      byName.put(StringUtils.simplify(value.getName()), value);
    }
  }

  public static final Enchantment PROTECTION = parse("PROTECTION_ENVIRONMENTAL", "protection");
  public static final Enchantment FIRE_PROTECTION = parse("PROTECTION_FIRE", "fire_protection");
  public static final Enchantment FEATHER_FALLING = parse("PROTECTION_FALL", "feather_falling");
  public static final Enchantment BLAST_PROTECTION =
      parse("PROTECTION_EXPLOSIONS", "blast_protection");
  public static final Enchantment PROJECTILE_PROTECTION =
      parse("PROTECTION_PROJECTILE", "projectile_protection");
  public static final Enchantment RESPIRATION = parse("OXYGEN", "respiration");
  public static final Enchantment AQUA_AFFINITY = parse("WATER_WORKER", "aqua_affinity");
  public static final Enchantment THORNS = Enchantment.THORNS;
  public static final Enchantment DEPTH_STRIDER = Enchantment.DEPTH_STRIDER;
  public static final Enchantment SHARPNESS = parse("DAMAGE_ALL", "sharpness");
  public static final Enchantment SMITE = parse("DAMAGE_UNDEAD", "smite");
  public static final Enchantment BANE_OF_ARTHROPODS =
      parse("DAMAGE_ARTHROPODS", "bane_of_arthropods");
  public static final Enchantment KNOCKBACK = Enchantment.KNOCKBACK;
  public static final Enchantment FIRE_ASPECT = Enchantment.FIRE_ASPECT;
  public static final Enchantment LOOTING = parse("LOOT_BONUS_MOBS", "looting");
  public static final Enchantment EFFICIENCY = parse("DIG_SPEED", "efficiency");
  public static final Enchantment SILK_TOUCH = Enchantment.SILK_TOUCH;
  public static final Enchantment UNBREAKING = parse("DURABILITY", "unbreaking");
  public static final Enchantment FORTUNE = parse("LOOT_BONUS_BLOCKS", "fortune");
  public static final Enchantment POWER = parse("ARROW_DAMAGE", "power");
  public static final Enchantment PUNCH = parse("ARROW_KNOCKBACK", "punch");
  public static final Enchantment FLAME = parse("ARROW_FIRE", "flame");
  public static final Enchantment INFINITY = parse("ARROW_INFINITE", "infinity");
  public static final Enchantment LUCK_OF_THE_SEA = parse("LUCK", "luck_of_the_sea");
  public static final Enchantment LURE = Enchantment.LURE;

  private static Enchantment parse(String... names) {
    Enchantment type = BukkitUtils.parse(Enchantment::getByName, names);
    for (String name : names) {
      byName.put(StringUtils.simplify(name), type);
    }
    return type;
  }

  public static Enchantment getByName(String name) {
    return byName.get(StringUtils.simplify(name));
  }
}
