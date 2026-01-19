package tc.oc.pgm.util.bukkit;

import java.util.HashMap;
import java.util.Map;
import org.bukkit.enchantments.Enchantment;
import tc.oc.pgm.util.StringUtils;

public class Enchantments {
  private static final Map<String, Enchantment> BY_NAME = new HashMap<>();

  static {
    parse("PROTECTION_ENVIRONMENTAL", "protection");
    parse("PROTECTION_FIRE", "fire_protection");
    parse("PROTECTION_FALL", "feather_falling");
    parse("PROTECTION_EXPLOSIONS", "blast_protection");
    parse("PROTECTION_PROJECTILE", "projectile_protection");
    parse("OXYGEN", "respiration");
    parse("WATER_WORKER", "aqua_affinity");
    parse("DAMAGE_UNDEAD", "smite");
    parse("DAMAGE_ARTHROPODS", "bane_of_arthropods");
    parse("LOOT_BONUS_MOBS", "looting");
    parse("DIG_SPEED", "efficiency");
    parse("DURABILITY", "unbreaking");
    parse("LOOT_BONUS_BLOCKS", "fortune");
    parse("ARROW_DAMAGE", "power");
    parse("ARROW_KNOCKBACK", "punch");
    parse("ARROW_FIRE", "flame");
    parse("LUCK", "luck_of_the_sea");

    for (Enchantment value : Enchantment.values()) {
      BY_NAME.put(StringUtils.simplify(value.getName()), value);
    }
  }

  public static final Enchantment SHARPNESS = parse("DAMAGE_ALL", "sharpness");
  public static final Enchantment INFINITY = parse("ARROW_INFINITE", "infinity");

  private static Enchantment parse(String... names) {
    Enchantment type = BukkitUtils.parse(Enchantment::getByName, names);
    for (String name : names) {
      BY_NAME.put(StringUtils.simplify(name), type);
    }
    return type;
  }

  public static Enchantment getByName(String name) {
    return BY_NAME.get(StringUtils.simplify(name));
  }
}
