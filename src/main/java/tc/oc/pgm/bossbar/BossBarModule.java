package tc.oc.pgm.bossbar;

import java.util.logging.Logger;
import org.jdom2.Document;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.map.MapModule;
import tc.oc.pgm.map.MapModuleContext;
import tc.oc.pgm.module.ModuleDescription;

@ModuleDescription(name = "Boss Name and Health Display")
public class BossBarModule extends MapModule<BossBarMatchModule> {

  @Override
  public BossBarMatchModule createMatchModule(Match match) {
    return new BossBarMatchModule(match);
  }

  public static BossBarModule parse(MapModuleContext context, Logger logger, Document doc) {
    return new BossBarModule();
  }
}
