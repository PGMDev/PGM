package tc.oc.pgm.api.attribute;

import java.util.HashMap;
import java.util.Map;

public enum Attribute {

  /** Maximum health of an Entity. */
  GENERIC_MAX_HEALTH("generic.maxHealth"),
  /** Range at which an Entity will follow others. */
  GENERIC_FOLLOW_RANGE("generic.followRange"),
  /** Resistance of an Entity to knockback. */
  GENERIC_KNOCKBACK_RESISTANCE("generic.knockbackResistance"),
  /** Movement speed of an Entity. */
  GENERIC_MOVEMENT_SPEED("generic.movementSpeed"),
  /** Attack damage of an Entity. */
  GENERIC_ATTACK_DAMAGE("generic.attackDamage"),
  /** Strength with which a horse will jump. */
  HORSE_JUMP_STRENGTH("horse.jumpStrength"),
  /** Chance of a zombie to spawn reinforcements. */
  ZOMBIE_SPAWN_REINFORCEMENTS("zombie.spawnReinforcements");

  private final String name;

  Attribute(String name) {
    this.name = name;
  }

  /** @return the external name of this attribute */
  public String getName() {
    return name;
  }

  private static final Map<String, Attribute> byName = new HashMap<>();

  static {
    for (Attribute attribute : values()) {
      byName.put(attribute.getName(), attribute);
    }
  }

  public static Attribute byName(String name) {
    return byName.get(name);
  }
}
