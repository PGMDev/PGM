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
import tc.oc.pgm.util.xml.XMLUtils;

public class TimeSetModule implements MapModule, MatchModule {
  private final Long time;

  public TimeSetModule(Long time) {
    this.time = time;
  }

  public Long getTime() {
    return this.time;
  }

  public static class Factory implements MapModuleFactory<TimeSetModule> {
    @Override
    public TimeSetModule parse(MapFactory factory, Logger logger, Document doc)
        throws InvalidXMLException {
      Element TimeSetEl = doc.getRootElement().getChild("world").getChild("timeset");
      if (TimeSetEl != null) {
        Long time = XMLUtils.parseNumber(TimeSetEl, Long.class);
        return new TimeSetModule(time);
      } else {
        return null;
      }
    }
  }

  @Override
  public MatchModule createMatchModule(Match match) throws ModuleLoadException {
    return this;
  }
}
