package tc.oc.pgm.modules;

import java.util.logging.Logger;
import org.jdom2.Document;
import org.jdom2.Element;
import tc.oc.pgm.api.map.MapModule;
import tc.oc.pgm.api.map.factory.MapFactory;
import tc.oc.pgm.api.map.factory.MapModuleFactory;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.match.MatchModule;
import tc.oc.pgm.api.module.exception.ModuleLoadException;
import tc.oc.xml.InvalidXMLException;

public class TimeLockModule implements MapModule {
  private final boolean lock;

  public TimeLockModule(boolean lock) {
    this.lock = lock;
  }

  public boolean isTimeLocked() {
    return this.lock;
  }

  public static class Factory implements MapModuleFactory<TimeLockModule> {
    @Override
    public TimeLockModule parse(MapFactory factory, Logger logger, Document doc)
        throws InvalidXMLException {
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

  @Override
  public MatchModule createMatchModule(Match match) throws ModuleLoadException {
    return null; // FIXME: PGMListener calls the TimeLockModule and asks, should have its own
    // MatchModule
  }
}
