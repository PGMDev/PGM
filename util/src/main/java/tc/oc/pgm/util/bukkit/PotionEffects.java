package tc.oc.pgm.util.bukkit;

import java.util.HashMap;
import java.util.Map;
import org.bukkit.potion.PotionEffectType;
import tc.oc.pgm.util.StringUtils;

public class PotionEffects {
  private static final Map<String, PotionEffectType> BY_NAME =
      new HashMap<>(PotionEffectType.values().length);

  static {
    for (PotionEffectType value : PotionEffectType.values()) {
      if (value != null) BY_NAME.put(StringUtils.simplify(value.getName()), value);
    }
  }

  public static final PotionEffectType SLOWNESS = parse("SLOW", "slowness");
  public static final PotionEffectType HASTE = parse("FAST_DIGGING", "HASTE");
  public static final PotionEffectType MINING_FATIGUE = parse("SLOW_DIGGING", "mining_fatigue");
  public static final PotionEffectType STRENGTH = parse("INCREASE_DAMAGE", "strength");
  public static final PotionEffectType INSTANT_HEALTH = parse("HEAL", "instant_health");
  public static final PotionEffectType INSTANT_DAMAGE = parse("HARM", "instant_damage");
  public static final PotionEffectType JUMP_BOOST = parse("JUMP", "jump_boost");
  public static final PotionEffectType NAUSEA = parse("CONFUSION", "nausea");
  public static final PotionEffectType RESISTANCE = parse("DAMAGE_RESISTANCE", "resistance");

  private static PotionEffectType parse(String... names) {
    PotionEffectType type = BukkitUtils.parse(PotionEffectType::getByName, names);
    for (String name : names) {
      BY_NAME.put(StringUtils.simplify(name), type);
    }
    return type;
  }

  public static PotionEffectType getByName(String name) {
    return BY_NAME.get(StringUtils.simplify(name));
  }
}
