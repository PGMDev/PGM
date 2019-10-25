package tc.oc.pgm.doublejump;

import java.util.logging.Logger;
import org.jdom2.Document;
import tc.oc.pgm.map.MapModule;
import tc.oc.pgm.map.MapModuleContext;
import tc.oc.pgm.match.Match;
import tc.oc.pgm.match.MatchModule;
import tc.oc.pgm.module.ModuleDescription;
import tc.oc.xml.InvalidXMLException;

@ModuleDescription(name = "Double Jump")
public class DoubleJumpModule extends MapModule {
  public MatchModule createMatchModule(Match match) {
    return new DoubleJumpMatchModule(match);
  }

  public static MapModule parse(MapModuleContext context, Logger logger, Document doc)
      throws InvalidXMLException {
    return new DoubleJumpModule();
  }
}
