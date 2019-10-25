package tc.oc.pgm.start;

import java.util.logging.Logger;
import org.jdom2.Document;
import tc.oc.pgm.bossbar.BossBarModule;
import tc.oc.pgm.map.MapModule;
import tc.oc.pgm.map.MapModuleContext;
import tc.oc.pgm.match.Match;
import tc.oc.pgm.match.MatchModule;
import tc.oc.pgm.module.ModuleDescription;
import tc.oc.xml.InvalidXMLException;

/** Match starting logic and commands */
@ModuleDescription(
    name = "match start",
    requires = {BossBarModule.class})
public class StartModule extends MapModule {

  public static StartModule parse(MapModuleContext context, Logger logger, Document doc)
      throws InvalidXMLException {
    return new StartModule();
  }

  @Override
  public MatchModule createMatchModule(Match match) {
    return new StartMatchModule(match);
  }
}
