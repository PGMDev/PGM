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

public class WorldTimeModule implements MapModule<WorldTimeModule>, MatchModule {
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
      // legacy
      Element timelockEl = doc.getRootElement().getChild("timelock");
      if (timelockEl != null) {
        lock = parseTimeLock(timelockEl);
      }

      Long time = null;
      boolean random = false;
      if (worldEl != null) {
        if (timelockEl == null) {
          timelockEl = worldEl.getChild("timelock");
          if (timelockEl != null) {
            lock = parseTimeLock(timelockEl);
          }
        }
        Element timeSetEl = worldEl.getChild("timeset");
        if (timeSetEl != null) {
          time = XMLUtils.parseNumber(timeSetEl, Long.class);
        }
        Element timeRandomEl = worldEl.getChild("randomtime");
        if (timeRandomEl != null) {
          random = true;
        }
      }
      return new WorldTimeModule(lock, time, random);
    }
  }

  public static boolean parseTimeLock(Element timelockEl) {
    return !timelockEl.getTextNormalize().equalsIgnoreCase("off");
  }

  @Override
  public WorldTimeModule createMatchModule(Match match) throws ModuleLoadException {
    return this;
  }
}
