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
    parse("DROPPED_ITEM", "ITEM");
    parse("LEASH_HITCH", "LEASH_KNOT");
    parse("ENDER_SIGNAL", "EYE_OF_ENDER");
    parse("THROWN_EXP_BOTTLE", "EXPERIENCE_BOTTLE");
    parse("FIREWORK", "FIREWORK_ROCKET");
    parse("MINECART_COMMAND", "COMMAND_BLOCK_MINECART");
    parse("MINECART_CHEST", "CHEST_MINECART");
    parse("MINECART_FURNACE", "FURNACE_MINECART");
    parse("MINECART_TNT", "TNT_MINECART");
    parse("MINECART_HOPPER", "HOPPER_MINECART");
    parse("MINECART_MOB_SPAWNER", "SPAWNER_MINECART");
    parse("PIG_ZOMBIE", "ZOMBIFIED_PIGLIN");
    parse("MUSHROOM_COW", "MOOSHROOM");
    parse("SNOWMAN", "SNOW_GOLEM");
    parse("SPLASH_POTION", "POTION");
    parse("FISHING_HOOK", "FISHING_BOBBER");
    parse("LIGHTNING", "LIGHTNING_BOLT");

    for (EntityType value : EntityType.values()) {
      BY_NAME.put(StringUtils.simplify(value.name()), value);
    }
  }

  public static final EntityType PRIMED_TNT = parse("PRIMED_TNT", "TNT");
  public static final EntityType ENDER_CRYSTAL = parse("ENDER_CRYSTAL", "END_CRYSTAL");
  public static final EntityType COMPLEX_PART = parse("COMPLEX_PART", "UNKNOWN");

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
