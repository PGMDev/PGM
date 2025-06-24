package tc.oc.pgm.util.bukkit;

import java.util.HashMap;
import java.util.Map;
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
  public static final EntityType EXPERIENCE_ORB = EntityType.EXPERIENCE_ORB;
  public static final EntityType LEASH_HITCH = parse("LEASH_HITCH", "LEASH_KNOT");
  public static final EntityType PAINTING = EntityType.PAINTING;
  public static final EntityType ARROW = EntityType.ARROW;
  public static final EntityType SNOWBALL = EntityType.SNOWBALL;
  public static final EntityType FIREBALL = EntityType.FIREBALL;
  public static final EntityType SMALL_FIREBALL = EntityType.SMALL_FIREBALL;
  public static final EntityType ENDER_PEARL = EntityType.ENDER_PEARL;
  public static final EntityType ENDER_SIGNAL = parse("ENDER_SIGNAL", "EYE_OF_ENDER");
  public static final EntityType THROWN_EXP_BOTTLE =
      parse("THROWN_EXP_BOTTLE", "EXPERIENCE_BOTTLE");
  public static final EntityType ITEM_FRAME = EntityType.ITEM_FRAME;
  public static final EntityType WITHER_SKULL = EntityType.WITHER_SKULL;
  public static final EntityType PRIMED_TNT = parse("PRIMED_TNT", "TNT");
  public static final EntityType FALLING_BLOCK = EntityType.FALLING_BLOCK;
  public static final EntityType FIREWORK = parse("FIREWORK", "FIREWORK_ROCKET");
  public static final EntityType ARMOR_STAND = EntityType.ARMOR_STAND;
  public static final EntityType MINECART_COMMAND =
      parse("MINECART_COMMAND", "COMMAND_BLOCK_MINECART");
  public static final EntityType BOAT = EntityType.BOAT;
  public static final EntityType MINECART = EntityType.MINECART;
  public static final EntityType MINECART_CHEST = parse("MINECART_CHEST", "CHEST_MINECART");
  public static final EntityType MINECART_FURNACE = parse("MINECART_FURNACE", "FURNACE_MINECART");
  public static final EntityType MINECART_TNT = parse("MINECART_TNT", "TNT_MINECART");
  public static final EntityType MINECART_HOPPER = parse("MINECART_HOPPER", "HOPPER_MINECART");
  public static final EntityType MINECART_MOB_SPAWNER =
      parse("MINECART_MOB_SPAWNER", "SPAWNER_MINECART");
  public static final EntityType CREEPER = EntityType.CREEPER;
  public static final EntityType SKELETON = EntityType.SKELETON;
  public static final EntityType SPIDER = EntityType.SPIDER;
  public static final EntityType GIANT = EntityType.GIANT;
  public static final EntityType ZOMBIE = EntityType.ZOMBIE;
  public static final EntityType SLIME = EntityType.SLIME;
  public static final EntityType GHAST = EntityType.GHAST;
  public static final EntityType PIG_ZOMBIE = parse("PIG_ZOMBIE", "ZOMBIFIED_PIGLIN");
  public static final EntityType ENDERMAN = EntityType.ENDERMAN;
  public static final EntityType CAVE_SPIDER = EntityType.CAVE_SPIDER;
  public static final EntityType SILVERFISH = EntityType.SILVERFISH;
  public static final EntityType BLAZE = EntityType.BLAZE;
  public static final EntityType MAGMA_CUBE = EntityType.MAGMA_CUBE;
  public static final EntityType ENDER_DRAGON = EntityType.ENDER_DRAGON;
  public static final EntityType WITHER = EntityType.WITHER;
  public static final EntityType BAT = EntityType.BAT;
  public static final EntityType WITCH = EntityType.WITCH;
  public static final EntityType ENDERMITE = EntityType.ENDERMITE;
  public static final EntityType GUARDIAN = EntityType.GUARDIAN;
  public static final EntityType PIG = EntityType.PIG;
  public static final EntityType SHEEP = EntityType.SHEEP;
  public static final EntityType COW = EntityType.COW;
  public static final EntityType CHICKEN = EntityType.CHICKEN;
  public static final EntityType SQUID = EntityType.SQUID;
  public static final EntityType WOLF = EntityType.WOLF;
  public static final EntityType MUSHROOM_COW = parse("MUSHROOM_COW", "MOOSHROOM");
  public static final EntityType SNOWMAN = parse("SNOWMAN", "SNOW_GOLEM");
  public static final EntityType OCELOT = EntityType.OCELOT;
  public static final EntityType IRON_GOLEM = EntityType.IRON_GOLEM;
  public static final EntityType HORSE = EntityType.HORSE;
  public static final EntityType RABBIT = EntityType.RABBIT;
  public static final EntityType VILLAGER = EntityType.VILLAGER;
  public static final EntityType ENDER_CRYSTAL = parse("ENDER_CRYSTAL", "END_CRYSTAL");
  public static final EntityType SPLASH_POTION = parse("SPLASH_POTION", "POTION");
  public static final EntityType EGG = EntityType.EGG;
  public static final EntityType FISHING_HOOK = parse("FISHING_HOOK", "FISHING_BOBBER");
  public static final EntityType LIGHTNING = parse("LIGHTNING", "LIGHTNING_BOLT");
  public static final EntityType PLAYER = EntityType.PLAYER;
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
}
