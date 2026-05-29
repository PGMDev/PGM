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
  }

  private <T> void setGameRule(GameRule<T> rule, @Nullable Object value) {
    rule.set(this.match.getWorld(), rule.type().cast(value));
  }

  public <T> T getGameRule(GameRule<T> rule) {
    return rule.type().cast(this.gameRules.get(rule));
  }
}
