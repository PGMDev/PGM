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
    parse("GENERIC_MAX_HEALTH", "MAX_HEALTH");
    parse("GENERIC_FOLLOW_RANGE", "FOLLOW_RANGE");
    parse("GENERIC_MOVEMENT_SPEED", "MOVEMENT_SPEED");
    parse("GENERIC_ATTACK_DAMAGE", "ATTACK_DAMAGE");
    parse("HORSE_JUMP_STRENGTH", "GENERIC_JUMP_STRENGTH", "JUMP_STRENGTH");
    parse("ZOMBIE_SPAWN_REINFORCEMENTS", "SPAWN_REINFORCEMENTS");
    if (Platform.isModern()) {
      parse("GENERIC_FLYING_SPEED", "FLYING_SPEED");
      parse("GENERIC_ATTACK_SPEED", "ATTACK_SPEED");
      parse("GENERIC_ARMOR", "ARMOR");
      parse("GENERIC_ARMOR_TOUGHNESS", "ARMOR_TOUGHNESS");
      parse("GENERIC_LUCK", "LUCK");
    }

    for (Attribute value : ATTRIBUTE_UTILS.getAttributeValues()) {
      if (value != null)
        BY_NAME.put(StringUtils.simplify(ATTRIBUTE_UTILS.getAttributeName(value)), value);
    }
  }

  public static Attribute KNOCKBACK_RESISTANCE =
      parse("GENERIC_KNOCKBACK_RESISTANCE", "KNOCKBACK_RESISTANCE");

  private static Attribute parse(String... names) {
    Attribute type = BukkitUtils.parse(ATTRIBUTE_UTILS::getAttributeValue, names);
    for (String name : names) {
      BY_NAME.put(StringUtils.simplify(name), type);
    }
    return type;
  }

  public static Attribute getByName(String name) {
    return BY_NAME.get(StringUtils.simplify(name));
  }
}
