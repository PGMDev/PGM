package tc.oc.pgm.gamerules;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMap;
import java.util.Map;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.match.MatchModule;

public class GameRulesMatchModule implements MatchModule {

  private final Match match;
  private final Map<GameRule<?>, Object> gameRules;

  public GameRulesMatchModule(Match match, Map<GameRule<?>, Object> gameRules) {
    this.match = match;
    this.gameRules = Preconditions.checkNotNull(gameRules, "gamerules");
  }

  @Override
  public void load() {
    for (Map.Entry<GameRule<?>, Object> gameRule : this.gameRules.entrySet()) {
      this.match
          .getWorld()
          .setGameRuleValue(gameRule.getKey().getName(), gameRule.getValue().toString());
    }
  }

  public ImmutableMap<GameRule<?>, Object> getGameRules() {
    return ImmutableMap.copyOf(gameRules);
  }
}
