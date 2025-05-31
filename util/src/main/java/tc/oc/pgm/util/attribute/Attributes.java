package tc.oc.pgm.util.attribute;

import static tc.oc.pgm.util.attribute.AttributeUtils.ATTRIBUTE_UTILS;

import java.util.HashMap;
import java.util.Map;
import org.bukkit.attribute.Attribute;
import tc.oc.pgm.util.StringUtils;
import tc.oc.pgm.util.bukkit.BukkitUtils;
import tc.oc.pgm.util.platform.Platform;

public class Attributes {

  private static final Map<String, Attribute> BY_NAME =
      new HashMap<>(ATTRIBUTE_UTILS.getAttributeValues().length);

  static {
    for (Attribute value : ATTRIBUTE_UTILS.getAttributeValues()) {
      if (value != null)
        BY_NAME.put(StringUtils.simplify(ATTRIBUTE_UTILS.getAttributeName(value)), value);
    }
  }

  // Only define attributes which existed <1.13 as that was the highest version ProjectAres servers
  // used. However, only parse 1.9+ attributes on modern servers.
  public static Attribute MAX_HEALTH = parse("GENERIC_MAX_HEALTH", "max_health");
  public static Attribute FOLLOW_RANGE = parse("GENERIC_FOLLOW_RANGE", "follow_range");
  public static Attribute KNOCKBACK_RESISTANCE =
      parse("GENERIC_KNOCKBACK_RESISTANCE", "knockback_resistance");
  public static Attribute MOVEMENT_SPEED = parse("GENERIC_MOVEMENT_SPEED", "movement_speed");
  public static Attribute FLYING_SPEED = parse(true, "GENERIC_FLYING_SPEED", "flying_speed");
  public static Attribute ATTACK_DAMAGE = parse("GENERIC_ATTACK_DAMAGE", "attack_damage");
  public static Attribute ATTACK_SPEED = parse(true, "GENERIC_ATTACK_SPEED", "attack_speed");
  public static Attribute ARMOR = parse(true, "GENERIC_ARMOR", "armor");
  public static Attribute ARMOR_TOUGHNESS =
      parse(true, "GENERIC_ARMOR_TOUGHNESS", "armor_toughness");
  public static Attribute LUCK = parse(true, "GENERIC_LUCK", "luck");
  public static Attribute JUMP_STRENGTH =
      parse("HORSE_JUMP_STRENGTH", "GENERIC_JUMP_STRENGTH", "jump_strength");
  public static Attribute SPAWN_REINFORCEMENTS =
      parse("ZOMBIE_SPAWN_REINFORCEMENTS", "spawn_reinforcements");

  private static Attribute parse(String... names) {
    Attribute type = BukkitUtils.parse(ATTRIBUTE_UTILS::getAttributeValue, names);
    for (String name : names) {
      BY_NAME.put(StringUtils.simplify(name), type);
    }
    return type;
  }

  private static Attribute parse(boolean modernAttribute, String... names) {
    if (modernAttribute && Platform.isModern()) return parse(names);
    else return null;
  }

  public static Attribute getByName(String name) {
    return BY_NAME.get(StringUtils.simplify(name));
  }
}
