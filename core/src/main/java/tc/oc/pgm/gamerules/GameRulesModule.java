package tc.oc.pgm.gamerules;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jspecify.annotations.NullMarked;
import tc.oc.pgm.api.map.MapModule;
import tc.oc.pgm.api.map.factory.MapFactory;
import tc.oc.pgm.api.map.factory.MapModuleFactory;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.modules.WorldTimeModule;
import tc.oc.pgm.util.Result;
import tc.oc.pgm.util.bukkit.GameRule;
import tc.oc.pgm.util.bukkit.GameRules;
import tc.oc.pgm.util.xml.InvalidXMLException;

@NullMarked
public class GameRulesModule implements MapModule<GameRulesMatchModule> {
  private final Map<GameRule<?>, Object> gameRules;

  private GameRulesModule(Map<GameRule<?>, Object> gameRules) {
    this.gameRules = gameRules;
  }

  public GameRulesMatchModule createMatchModule(Match match) {
    return new GameRulesMatchModule(match, this.gameRules);
  }

  public static class Factory implements MapModuleFactory<GameRulesModule> {
    @Override
    public Collection<Class<? extends MapModule<?>>> getSoftDependencies() {
      return List.of(WorldTimeModule.class);
    }

    @Override
    public GameRulesModule parse(MapFactory factory, Logger logger, Document doc)
        throws InvalidXMLException {
      Map<GameRule<?>, Object> gameRules = new HashMap<>();

      for (Element gameRulesElement : doc.getRootElement().getChildren("gamerules")) {
        for (Element gameRuleElement : gameRulesElement.getChildren()) {
          String ruleName = gameRuleElement.getName();
          String value = gameRuleElement.getValue();

          if (value == null) {
            throw new InvalidXMLException(
                "Missing value for game rule " + ruleName, gameRuleElement);
          }

          GameRule<?> rule = GameRules.getByName(ruleName);
          if (rule == null) {
            logger.log(
                Level.WARNING,
                null,
                new InvalidXMLException(
                    "Game rule " + ruleName + " does not exist or is unsupported by the platform",
                    gameRuleElement));
            continue;
          } else if (gameRules.containsKey(rule)) {
            throw new InvalidXMLException(
                rule.name() + " has already been specified", gameRuleElement);
          }

          var maybeConflict = gameRules.keySet().stream()
              .filter(Predicate.not(rule::canBeCombinedWith))
              .findFirst();

          if (maybeConflict.isPresent()) {
            throw new InvalidXMLException(
                "Game rule " + rule.name() + " cannot be combined with "
                    + maybeConflict.get().name(),
                gameRuleElement);
          }

          switch (rule.tryParse(value)) {
            case Result.Ok<?, ?>(Object parsed) -> gameRules.put(rule, parsed);
            case Result.Err<?, ?>(Throwable err) ->
              throw new InvalidXMLException(
                  "Failed to parse game rule value for " + rule.name() + ": " + err.getMessage(),
                  gameRuleElement);
          }
        }
      }
      return new GameRulesModule(gameRules);
    }
  }
}
