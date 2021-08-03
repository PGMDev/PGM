package tc.oc.pgm.gamerules;

import com.google.common.base.Preconditions;
import java.util.Map;
import tc.oc.pgm.api.map.GameRule;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.match.MatchModule;
import tc.oc.pgm.modules.WorldTimeModule;

public class GameRulesMatchModule implements MatchModule {

  private final Match match;
  private final Map<String, String> gameRules;

  public GameRulesMatchModule(Match match, Map<String, String> gameRules) {
    this.match = match;
    this.gameRules = Preconditions.checkNotNull(gameRules, "gamerules");
  }

  @Override
  public void load() {
    // saves gamerules from world (level.dat) as fallback
    for (String gameRule : this.match.getWorld().getGameRules()) {
      gameRules.put(gameRule, this.match.getWorld().getGameRuleValue(gameRule));
    }

    // saves and sets gamerules from XML
    for (Map.Entry<String, String> gameRule : this.gameRules.entrySet()) {
      gameRules.put(gameRule.getKey(), this.match.getWorld().getGameRuleValue(gameRule.getValue()));
      this.match.getWorld().setGameRuleValue(gameRule.getKey(), gameRule.getValue());
    }

    // if timelock is off, save doDayLightCycle as true
    WorldTimeModule wtm = this.match.getModule(WorldTimeModule.class);
    if (wtm != null && !wtm.isTimeLocked()) {
      gameRules.put(GameRule.DO_DAYLIGHT_CYCLE.getId(), Boolean.toString(true));
    }
  }

  public String getGameRule(String gameRule) {
    return gameRules.get(gameRule);
  }
}
