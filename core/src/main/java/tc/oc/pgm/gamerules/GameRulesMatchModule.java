package tc.oc.pgm.gamerules;

import static tc.oc.pgm.util.Assert.assertNotNull;
import static tc.oc.pgm.util.bukkit.MiscUtils.MISC_UTILS;

import java.util.Map;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.match.MatchModule;
import tc.oc.pgm.modules.WorldTimeModule;
import tc.oc.pgm.util.bukkit.GameRules;

public class GameRulesMatchModule implements MatchModule {

  private final Match match;
  private final Map<String, String> gameRules;

  public GameRulesMatchModule(Match match, Map<String, String> gameRules) {
    this.match = match;
    this.gameRules = assertNotNull(gameRules, "gamerules");
  }

  @Override
  public void load() {
    // first, set doDaylightCycle according to timelock
    WorldTimeModule wtm = this.match.getModule(WorldTimeModule.class);
    MISC_UTILS.setGameRule(
        this.match.getWorld(),
        GameRules.ADVANCE_TIME,
        Boolean.toString(wtm != null && !wtm.isTimeLocked()));

    // second, set any gamerules defined in the map's XML
    // doDaylightCycle set in XML's gamerules module will take precedence over timelock
    for (Map.Entry<String, String> gameRule : this.gameRules.entrySet()) {
      MISC_UTILS.setGameRule(this.match.getWorld(), gameRule.getKey(), gameRule.getValue());
    }

    // lastly, retrieve gamerules from the map's level.dat and set them if absent
    for (String gameRule : this.match.getWorld().getGameRules()) {
      gameRules.putIfAbsent(gameRule, this.match.getWorld().getGameRuleValue(gameRule));
    }
  }

  public String getGameRule(String gameRule) {
    return gameRules.get(gameRule);
  }
}
