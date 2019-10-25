package tc.oc.pgm.goals;

import java.util.logging.Logger;
import org.jdom2.Document;
import tc.oc.component.Component;
import tc.oc.component.types.PersonalizedTranslatable;
import tc.oc.pgm.map.MapModule;
import tc.oc.pgm.map.MapModuleContext;
import tc.oc.pgm.match.Match;
import tc.oc.pgm.match.MatchModule;
import tc.oc.pgm.module.ModuleDescription;
import tc.oc.xml.InvalidXMLException;

@ModuleDescription(name = "goals")
public class GoalModule extends MapModule {

  @Override
  public MatchModule createMatchModule(Match match) {
    return new GoalMatchModule(match);
  }

  private static final Component GAME =
      new PersonalizedTranslatable("match.scoreboard.objectives.title");

  @Override
  public Component getGame(MapModuleContext context) {
    return GAME;
  }

  public static GoalModule parse(MapModuleContext context, Logger logger, Document doc)
      throws InvalidXMLException {
    return new GoalModule();
  }
}
