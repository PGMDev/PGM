package tc.oc.pgm.bossbar;

import java.util.logging.Logger;
import org.jdom2.Document;
import tc.oc.pgm.api.map.MapContext;
import tc.oc.pgm.api.map.MapModule;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.match.MatchModule;

public class BossBarModule implements MapModule {

  @Override
  public MatchModule createMatchModule(Match match) {
    return new BossBarMatchModule(match);
  }

  public static BossBarModule parse(MapContext context, Logger logger, Document doc) {
    return new BossBarModule();
  }
}
