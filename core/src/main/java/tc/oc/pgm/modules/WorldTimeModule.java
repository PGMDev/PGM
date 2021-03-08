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

public class WorldTimeModule implements MapModule, MatchModule {
  private final boolean lock;
  private final Long time;
  private final boolean random;

  public WorldTimeModule(boolean lock, Long time, boolean random) {
    this.lock = lock;
    this.time = time;
    this.random = random;
  }

  public boolean isTimeLocked() {
    return this.lock;
  }

  public Long getTime() {
    return this.time;
  }

  public boolean isTimeRandom() {
    return this.random;
  }

  public static class Factory implements MapModuleFactory<WorldTimeModule> {
    @Override
    public WorldTimeModule parse(MapFactory factory, Logger logger, Document doc)
        throws InvalidXMLException {
      boolean lock = true;
      Element worldEl = doc.getRootElement().getChild("world");
      Element timelockEl = worldEl.getChild("timelock");
      // legacy
      if (timelockEl == null) {
        timelockEl = doc.getRootElement().getChild("timelock");
      }
      if (timelockEl != null) {
        if (timelockEl.getTextNormalize().equalsIgnoreCase("off")) {
          lock = false;
        }
      }

      Element TimeSetEl = worldEl.getChild("timeset");
      Long time = null;
      if (TimeSetEl != null) {
        time = XMLUtils.parseNumber(TimeSetEl, Long.class);
      }

      Element TimeRandomEl = worldEl.getChild("randomtime");
      boolean random = false;
      if (TimeRandomEl != null) {
        random = true;
      }
      return new WorldTimeModule(lock, time, random);
    }
  }

  @Override
  public MatchModule createMatchModule(Match match) throws ModuleLoadException {
    return this;
  }
}
