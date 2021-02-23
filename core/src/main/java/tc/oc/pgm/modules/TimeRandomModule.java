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
import tc.oc.pgm.util.xml.InvalidXMLException;

public class TimeRandomModule implements MapModule, MatchModule {
  private final boolean random;

  public TimeRandomModule(boolean random) {
    this.random = random;
  }

  public boolean isTimeRandom() {
    return random;
  }

  public static class Factory implements MapModuleFactory<TimeRandomModule> {
    @Override
    public TimeRandomModule parse(MapFactory factory, Logger logger, Document doc)
        throws InvalidXMLException {
      Element TimeRandomEl = doc.getRootElement().getChild("randomtime");
      if (TimeRandomEl != null) {
        return new TimeRandomModule(true);
      } else {
        return new TimeRandomModule(false);
      }
    }
  }

  @Override
  public MatchModule createMatchModule(Match match) throws ModuleLoadException {
    return this;
  }
}
