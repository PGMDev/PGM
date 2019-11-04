package tc.oc.pgm.modules;

import java.util.logging.Logger;
import org.jdom2.Document;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.map.MapModule;
import tc.oc.pgm.map.MapModuleContext;
import tc.oc.pgm.match.MatchModule;
import tc.oc.pgm.module.ModuleDescription;

@ModuleDescription(name = "ArrowRemoval")
public class ArrowRemovalModule extends MapModule {
  @Override
  public MatchModule createMatchModule(Match match) {
    return new ArrowRemovalMatchModule(match);
  }

  public static ArrowRemovalModule parse(MapModuleContext context, Logger logger, Document doc) {
    return new ArrowRemovalModule();
  }
}
