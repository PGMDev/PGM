package tc.oc.pgm.gamerules;

import com.google.common.collect.ImmutableMap;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;
import org.jdom2.Document;
import org.jdom2.Element;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.map.MapModule;
import tc.oc.pgm.map.MapModuleContext;
import tc.oc.pgm.match.MatchModule;
import tc.oc.pgm.module.ModuleDescription;
import tc.oc.xml.InvalidXMLException;

@ModuleDescription(name = "Gamerules")
public class GameRulesModule extends MapModule {

  private Map<GameRule, Boolean> gameRules;

  private GameRulesModule(Map<GameRule, Boolean> gamerules) {
    this.gameRules = gamerules;
  }

  public MatchModule createMatchModule(Match match) {
    return new GameRulesMatchModule(match, this.gameRules);
  }

  // ---------------------
  // ---- XML Parsing ----
  // ---------------------

  public static GameRulesModule parse(MapModuleContext context, Logger logger, Document doc)
      throws InvalidXMLException {
    Map<GameRule, Boolean> gameRules = new HashMap<>();

    for (Element gameRulesElement : doc.getRootElement().getChildren("gamerules")) {
      for (Element gameRuleElement : gameRulesElement.getChildren()) {
        GameRule gameRule = GameRule.forName(gameRuleElement.getName());
        String value = gameRuleElement.getValue();

        if (gameRule == null) {
          throw new InvalidXMLException(
              gameRuleElement.getName() + " is not a valid gamerule", gameRuleElement);
        }
        if (value == null) {
          throw new InvalidXMLException(
              "Missing value for gamerule " + gameRule.getValue(), gameRuleElement);
        } else if (!(value.equalsIgnoreCase("true") || value.equalsIgnoreCase("false"))) {
          throw new InvalidXMLException(
              gameRuleElement.getValue() + " is not a valid gamerule value", gameRuleElement);
        }
        if (gameRules.containsKey(gameRule)) {
          throw new InvalidXMLException(
              gameRule.getValue() + " has already been specified", gameRuleElement);
        }

        gameRules.put(gameRule, Boolean.valueOf(value));
      }
    }
    return new GameRulesModule(gameRules);
  }

  public ImmutableMap<GameRule, Boolean> getGameRules() {
    return ImmutableMap.copyOf(this.gameRules);
  }
}
