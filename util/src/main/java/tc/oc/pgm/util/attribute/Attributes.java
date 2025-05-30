package tc.oc.pgm.util.attribute;

import static tc.oc.pgm.util.attribute.AttributeUtils.ATTRIBUTE_UTILS;

import java.util.HashMap;
import java.util.Map;
import org.bukkit.attribute.Attribute;
import tc.oc.pgm.util.StringUtils;
import tc.oc.pgm.util.bukkit.BukkitUtils;

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
  // used
  public static Attribute MAX_HEALTH = parse("generic.maxHealth", "maxHealth", "max_health");
  public static Attribute FOLLOW_RANGE =
      parse("generic.followRange", "followRange", "follow_range");
  public static Attribute KNOCKBACK_RESISTANCE =
      parse("generic.knockbackResistance", "knockbackResistance", "knockback_resistance");
  public static Attribute MOVEMENT_SPEED =
      parse("generic.movementSpeed", "movementSpeed", "movement_speed");
  public static Attribute FLYING_SPEED =
      parse("generic.flyingSpeed", "flyingSpeed", "flying_speed");
  public static Attribute ATTACK_DAMAGE =
      parse("generic.attackDamage", "attackDamage", "attack_damage");
  public static Attribute ATTACK_SPEED =
      parse("generic.attackSpeed", "attackSpeed", "attack_speed");
  public static Attribute ARMOR = parse("generic.armor", "armor");
  public static Attribute ARMOR_TOUGHNESS =
      parse("generic.armorToughness", "armorToughness", "armor_toughness");
  public static Attribute LUCK = parse("generic.luck", "luck");
  public static Attribute JUMP_STRENGTH = parse("horse.jumpStrength", "jump_strength");
  public static Attribute SPAWN_REINFORCEMENTS =
      parse("zombie.spawnReinforcements", "spawn_reinforcements");

  public static Attribute parse(String... names) {
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
