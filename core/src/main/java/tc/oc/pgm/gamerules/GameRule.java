package tc.oc.pgm.gamerules;

import static com.google.common.base.Preconditions.*;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.function.Predicate;

public class GameRule<T> {
  private static final Map<String, GameRule<?>> RULES_BY_NAME = new HashMap<>();

  public static final GameRule<Boolean> DO_DAYLIGHT_CYCLE =
      register(new BooleanGameRule("doDaylightCycle"));
  public static final GameRule<Boolean> DO_ENTITY_DROPS =
      register(new BooleanGameRule("doEntityDrops"));
  public static final GameRule<Boolean> DO_FIRE_TICK = register(new BooleanGameRule("doFireTick"));
  public static final GameRule<Boolean> DO_MOB_LOOT = register(new BooleanGameRule("doMobLoot"));
  public static final GameRule<Boolean> DO_MOB_SPAWNING =
      register(new BooleanGameRule("doMobSpawning"));
  public static final GameRule<Boolean> DO_TILE_DROPS =
      register(new BooleanGameRule("doTileDrops"));
  public static final GameRule<Boolean> KEEP_INVENTORY =
      register(new BooleanGameRule("keepInventory"));
  public static final GameRule<Boolean> MOB_GRIEFING = register(new BooleanGameRule("mobGriefing"));
  public static final GameRule<Boolean> NATURAL_REGENERATION =
      register(new BooleanGameRule("naturalRegeneration"));
  public static final GameRule<Integer> RANDOM_TICK_SPEED =
      register(new IntegerGameRule("randomTickSpeed"));
  public static final GameRule<Boolean> REDUCED_DEBUG_INFO =
      register(new BooleanGameRule("reducedDebugInfo"));

  private static <T> GameRule<T> register(GameRule<T> gameRule) {
    RULES_BY_NAME.put(gameRule.name.toLowerCase(Locale.ROOT), gameRule);
    return gameRule;
  }

  private final String name;
  private final Predicate<String> test;

  GameRule(String name, Predicate<String> test) {
    this.name = checkNotNull(name);
    this.test = checkNotNull(test);
  }

  public String getName() {
    return this.name;
  }

  public Predicate<String> getParseTest() {
    return this.test;
  }

  public static GameRule<?> forName(String query) {
    return RULES_BY_NAME.get(query.toLowerCase(Locale.ROOT));
  }

  private static class BooleanGameRule extends GameRule<Boolean> {
    BooleanGameRule(String name) {
      super(name, string -> string.equalsIgnoreCase("true") || string.equalsIgnoreCase("false"));
    }
  }

  private static class IntegerGameRule extends GameRule<Integer> {
    IntegerGameRule(String name) {
      super(
          name,
          string -> {
            try {
              Integer.parseInt(string);
              return true;
            } catch (NumberFormatException e) {
              return false;
            }
          });
    }
  }
}
