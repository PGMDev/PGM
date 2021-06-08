package tc.oc.pgm.gamerules;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMap;
import java.util.HashMap;
import java.util.Map;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.match.MatchModule;

public class GameRulesMatchModule implements MatchModule {

  private final Match match;
  private final Map<String, String> gameRules;
  private Map<String, String> mapGameRules = new HashMap<>();

  public GameRulesMatchModule(Match match, Map<String, String> gameRules) {
    this.match = match;
    this.gameRules = Preconditions.checkNotNull(gameRules, "gamerules");
  }

  @Override
  public void load() {
    for (String gameRule : this.match.getWorld().getGameRules()) {
      mapGameRules.put(gameRule, this.match.getWorld().getGameRuleValue(gameRule));
    }

    for (Map.Entry<String, String> gameRule : this.gameRules.entrySet()) {
      this.match.getWorld().setGameRuleValue(gameRule.getKey(), gameRule.getValue());
    }
  }

  public ImmutableMap<String, String> getGameRules() {
    return ImmutableMap.copyOf(gameRules);
  }

  public ImmutableMap<String, String> getMapGameRules() {
    return ImmutableMap.copyOf(mapGameRules);
  }
}
