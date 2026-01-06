package tc.oc.pgm.util.bukkit;

import static tc.oc.pgm.util.Assert.assertNotNull;

import java.util.Map;
import java.util.Set;
import org.jetbrains.annotations.Unmodifiable;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import tc.oc.pgm.util.platform.Platform;

@NullMarked
public class GameRules {
  public interface GameRuleRegistry {
    @Unmodifiable
    Map<String, GameRule<?>> getGameRules();
  }

  private static final @Unmodifiable Map<String, GameRule<?>> KNOWN_GAME_RULES =
      Platform.get(GameRuleRegistry.class).getGameRules();

  private static final @Unmodifiable Set<GameRule<?>> KNOWN_GAME_RULES_SET =
      Set.copyOf(KNOWN_GAME_RULES.values());

  public static GameRule<Boolean> ADVANCE_TIME = assertNotNull(getByName("advance_time"));
  public static GameRule<Integer> FIRE_SPREAD_RADIUS_AROUND_PLAYER =
      assertNotNull(getByName("fire_spread_radius_around_player"));
  public static GameRule<Integer> RANDOM_TICK_SPEED = assertNotNull(getByName("random_tick_speed"));

  @SuppressWarnings("unchecked")
  public static <T> @Nullable GameRule<T> getByName(String name) {
    return (GameRule<T>) KNOWN_GAME_RULES.get(name);
  }

  public static @Unmodifiable Set<GameRule<?>> getKnownGameRules() {
    return KNOWN_GAME_RULES_SET;
  }
}
