package tc.oc.pgm.scoreboard;

import java.util.logging.Logger;
import org.jdom2.Document;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.ffa.FreeForAllModule;
import tc.oc.pgm.map.MapModule;
import tc.oc.pgm.map.MapModuleContext;
import tc.oc.pgm.match.MatchModule;
import tc.oc.pgm.module.ModuleDescription;
import tc.oc.pgm.teams.TeamModule;
import tc.oc.xml.InvalidXMLException;

@ModuleDescription(
    name = "scoreboard",
    follows = {TeamModule.class, FreeForAllModule.class})
public class ScoreboardModule extends MapModule {

  @Override
  public MatchModule createMatchModule(Match match) {
    return new ScoreboardMatchModule(match);
  }

  public static ScoreboardModule parse(MapModuleContext context, Logger logger, Document doc)
      throws InvalidXMLException {
    return new ScoreboardModule();
  }
}
