package tc.oc.pgm.gamerules;

import com.google.common.base.Preconditions;
import java.util.Map;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.match.MatchModule;

public class GameRulesMatchModule implements MatchModule {

  private final Match match;
  private final Map<String, String> gameRules;

  public GameRulesMatchModule(Match match, Map<String, String> gameRules) {
    this.match = match;
    this.gameRules = Preconditions.checkNotNull(gameRules, "gamerules");
  }

  @Override
  public void load() {
    for (Map.Entry<String, String> gameRule : this.gameRules.entrySet()) {
      this.match.getWorld().setGameRuleValue(gameRule.getKey(), gameRule.getValue());
    }

    // gets gamerule values from level.dat after being set
    // does not save doDaylightCycle to gameRules
    for (String gameRule : this.match.getWorld().getGameRules()) {
      if (!gameRule.equals("doDaylightCycle")) {
        gameRules.put(gameRule, this.match.getWorld().getGameRuleValue(gameRule));
      }
    }
  }

  public String getGameRule(String gameRule) {
    return gameRules.get(gameRule);
  }
}
