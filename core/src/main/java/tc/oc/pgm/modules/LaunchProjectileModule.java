package tc.oc.pgm.modules;

import java.util.logging.Logger;
import org.jdom2.Document;
import tc.oc.pgm.api.map.MapModule;
import tc.oc.pgm.api.map.factory.MapFactory;
import tc.oc.pgm.api.map.factory.MapModuleFactory;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.util.xml.InvalidXMLException;

public class LaunchProjectileModule implements MapModule<LaunchProjectileMatchModule> {

  @Override
  public LaunchProjectileMatchModule createMatchModule(Match match) {
    return new LaunchProjectileMatchModule(match);
  }

  public static class Factory implements MapModuleFactory<LaunchProjectileModule> {
    @Override
    public LaunchProjectileModule parse(MapFactory factory, Logger logger, Document doc)
        throws InvalidXMLException {
      return new LaunchProjectileModule();
    }
  }
}
