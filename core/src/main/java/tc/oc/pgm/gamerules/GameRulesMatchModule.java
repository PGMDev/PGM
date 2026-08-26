package tc.oc.pgm.gamerules;

import java.util.Map;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.match.MatchModule;
import tc.oc.pgm.modules.WorldTimeModule;
import tc.oc.pgm.util.bukkit.GameRule;
import tc.oc.pgm.util.bukkit.GameRules;

@NullMarked
public class GameRulesMatchModule implements MatchModule {
  // Gamerules forced to a safe value whenever the match is not running
  private static final Map<GameRule<?>, Object> IDLE_GAME_RULES =
      Map.of(GameRules.ADVANCE_TIME, false, GameRules.FIRE_SPREAD_RADIUS_AROUND_PLAYER, 0);

  private final Match match;
  private final Map<GameRule<?>, @Nullable Object> gameRules;

  public GameRulesMatchModule(Match match, Map<GameRule<?>, Object> gameRules) {
    this.match = match;
    this.gameRules = gameRules;
  }

  @Override
  public void load() {
    // first, set doDaylightCycle according to timelock
    WorldTimeModule wtm = this.match.getModule(WorldTimeModule.class);
    GameRules.ADVANCE_TIME.set(this.match.getWorld(), wtm != null && !wtm.isTimeLocked());

    // second, set any gamerules defined in the map's XML
    // doDaylightCycle set in XML's gamerules module will take precedence over timelock
    for (var entry : this.gameRules.entrySet()) {
      setGameRule(entry.getKey(), entry.getValue());
    }

    for (GameRule<?> rule : GameRules.getKnownGameRules()) {
      gameRules.put(rule, rule.get(this.match.getWorld()));
    }

    IDLE_GAME_RULES.forEach(this::setGameRule);
  }

  @Override
  public void enable() {
    IDLE_GAME_RULES.keySet().forEach(rule -> setGameRule(rule, this.gameRules.get(rule)));
  }

  @Override
  public void disable() {
    IDLE_GAME_RULES.forEach(this::setGameRule);
  }

  private <T> void setGameRule(GameRule<T> rule, @Nullable Object value) {
    rule.set(this.match.getWorld(), rule.type().cast(value));
  }

  public <T> T getGameRule(GameRule<T> rule) {
    return rule.type().cast(this.gameRules.get(rule));
  }
}
