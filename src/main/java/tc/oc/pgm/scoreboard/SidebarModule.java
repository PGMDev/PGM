package tc.oc.pgm.scoreboard;

import java.util.logging.Logger;
import org.jdom2.Document;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.blitz.BlitzModule;
import tc.oc.pgm.map.MapModule;
import tc.oc.pgm.map.MapModuleContext;
import tc.oc.pgm.module.ModuleDescription;
import tc.oc.pgm.score.ScoreModule;
import tc.oc.pgm.teams.TeamModule;
import tc.oc.pgm.wool.WoolModule;
import tc.oc.xml.InvalidXMLException;

@ModuleDescription(
    name = "sidebar",
    requires = {ScoreboardModule.class},
    follows = {TeamModule.class, ScoreModule.class, BlitzModule.class, WoolModule.class})
public class SidebarModule extends MapModule<SidebarMatchModule> {

  @Override
  public SidebarMatchModule createMatchModule(Match match) {
    return new SidebarMatchModule(match);
  }

  public static SidebarModule parse(MapModuleContext context, Logger logger, Document doc)
      throws InvalidXMLException {
    return new SidebarModule();
  }
}
