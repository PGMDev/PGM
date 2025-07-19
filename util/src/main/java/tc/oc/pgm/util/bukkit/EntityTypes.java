package tc.oc.pgm.util.bukkit;

import java.util.HashMap;
import java.util.Map;
import org.bukkit.entity.Boat;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import tc.oc.pgm.util.StringUtils;

public class EntityTypes {
  private static final Map<String, EntityType> BY_NAME = new HashMap<>();

  static {
    for (EntityType value : EntityType.values()) {
      BY_NAME.put(StringUtils.simplify(value.name()), value);
    }
  }

  public static final EntityType DROPPED_ITEM = parse("DROPPED_ITEM", "ITEM");
  public static final EntityType LEASH_HITCH = parse("LEASH_HITCH", "LEASH_KNOT");
  public static final EntityType ENDER_SIGNAL = parse("ENDER_SIGNAL", "EYE_OF_ENDER");
  public static final EntityType THROWN_EXP_BOTTLE =
      parse("THROWN_EXP_BOTTLE", "EXPERIENCE_BOTTLE");
  public static final EntityType PRIMED_TNT = parse("PRIMED_TNT", "TNT");
  public static final EntityType FIREWORK = parse("FIREWORK", "FIREWORK_ROCKET");
  public static final EntityType MINECART_COMMAND =
      parse("MINECART_COMMAND", "COMMAND_BLOCK_MINECART");
  public static final EntityType MINECART_CHEST = parse("MINECART_CHEST", "CHEST_MINECART");
  public static final EntityType MINECART_FURNACE = parse("MINECART_FURNACE", "FURNACE_MINECART");
  public static final EntityType MINECART_TNT = parse("MINECART_TNT", "TNT_MINECART");
  public static final EntityType MINECART_HOPPER = parse("MINECART_HOPPER", "HOPPER_MINECART");
  public static final EntityType MINECART_MOB_SPAWNER =
      parse("MINECART_MOB_SPAWNER", "SPAWNER_MINECART");
  public static final EntityType PIG_ZOMBIE = parse("PIG_ZOMBIE", "ZOMBIFIED_PIGLIN");
  public static final EntityType MUSHROOM_COW = parse("MUSHROOM_COW", "MOOSHROOM");
  public static final EntityType SNOWMAN = parse("SNOWMAN", "SNOW_GOLEM");
  public static final EntityType ENDER_CRYSTAL = parse("ENDER_CRYSTAL", "END_CRYSTAL");
  public static final EntityType SPLASH_POTION = parse("SPLASH_POTION", "POTION");
  public static final EntityType FISHING_HOOK = parse("FISHING_HOOK", "FISHING_BOBBER");
  public static final EntityType LIGHTNING = parse("LIGHTNING", "LIGHTNING_BOLT");
  public static final EntityType COMPLEX_PART = parse("COMPLEX_PART", "UNKNOWN");
  public static final EntityType UNKNOWN = EntityType.UNKNOWN;

  private static EntityType parse(String... names) {
    EntityType type = BukkitUtils.parse(EntityType::valueOf, names);
    for (String name : names) {
      BY_NAME.put(StringUtils.simplify(name), type);
    }
    return type;
  }

  public static EntityType getByName(String name) {
    return BY_NAME.get(StringUtils.simplify(name));
  }

  public static Class<? extends Entity> getClassByName(String name) {
    var type = getByName(name);
    if (type != null) return type.getEntityClass();

    // In modern Boat type is split into each wood, but common class still exists.
    if ("boat".equalsIgnoreCase(name)) {
      return Boat.class;
    }
    return null;
  }
}
