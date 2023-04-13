package tc.oc.pgm.gamerules;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;
import org.jdom2.Document;
import org.jdom2.Element;
import tc.oc.pgm.api.map.MapModule;
import tc.oc.pgm.api.map.factory.MapFactory;
import tc.oc.pgm.api.map.factory.MapModuleFactory;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.modules.WorldTimeModule;
import tc.oc.pgm.util.xml.InvalidXMLException;

public class GameRulesModule implements MapModule<GameRulesMatchModule> {

  private Map<String, String> gameRules;

  private GameRulesModule(Map<String, String> gamerules) {
    this.gameRules = gamerules;
  }

  public GameRulesMatchModule createMatchModule(Match match) {
    return new GameRulesMatchModule(match, this.gameRules);
  }

  public static class Factory implements MapModuleFactory<GameRulesModule> {
    @Override
    public Collection<Class<? extends MapModule<?>>> getSoftDependencies() {
      return ImmutableList.of(WorldTimeModule.class);
    }

    @Override
    public GameRulesModule parse(MapFactory factory, Logger logger, Document doc)
        throws InvalidXMLException {
      Map<String, String> gameRules = new HashMap<>();

      for (Element gameRulesElement : doc.getRootElement().getChildren("gamerules")) {
        for (Element gameRuleElement : gameRulesElement.getChildren()) {
          String rule = gameRuleElement.getName();
          String value = gameRuleElement.getValue();

          if (value == null) {
            throw new InvalidXMLException("Missing value for gamerule " + rule, gameRuleElement);
          } else if (gameRules.containsKey(rule)) {
            throw new InvalidXMLException(rule + " has already been specified", gameRuleElement);
          }

          gameRules.put(rule, value);
        }
      }
      return new GameRulesModule(gameRules);
    }
  }

  public ImmutableMap<String, String> getGameRules() {
    return ImmutableMap.copyOf(this.gameRules);
  }
}
