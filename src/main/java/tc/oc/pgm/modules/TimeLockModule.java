package tc.oc.pgm.modules;

import java.util.logging.Logger;
import org.jdom2.Document;
import org.jdom2.Element;
import tc.oc.pgm.map.MapModule;
import tc.oc.pgm.map.MapModuleContext;
import tc.oc.pgm.match.MatchModule;
import tc.oc.pgm.module.ModuleDescription;

@ModuleDescription(name = "Time Lock")
public class TimeLockModule extends MapModule<MatchModule> {
  protected final boolean lock;

  public TimeLockModule(boolean lock) {
    this.lock = lock;
  }

  public boolean isTimeLocked() {
    return this.lock;
  }

  public static TimeLockModule parse(MapModuleContext context, Logger logger, Document doc) {
    boolean lock = true;
    Element timelockEl = doc.getRootElement().getChild("timelock");
    if (timelockEl != null) {
      if (timelockEl.getTextNormalize().equalsIgnoreCase("off")) {
        lock = false;
      }
    }
    return new TimeLockModule(lock);
  }
}
