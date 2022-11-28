package tc.oc.pgm.rage;

import com.google.common.collect.ImmutableList;
import java.util.Collection;
import java.util.logging.Logger;
import org.jdom2.Document;
import tc.oc.pgm.api.map.MapModule;
import tc.oc.pgm.api.map.MapTag;
import tc.oc.pgm.api.map.factory.MapFactory;
import tc.oc.pgm.api.map.factory.MapModuleFactory;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.util.xml.InvalidXMLException;

public class RageModule implements MapModule<RageMatchModule> {
  private static final Collection<MapTag> TAGS =
      ImmutableList.of(new MapTag("rage", "Rage", true, true));

  @Override
  public RageMatchModule createMatchModule(Match match) {
    return new RageMatchModule(match);
  }

  @Override
  public Collection<MapTag> getTags() {
    return TAGS;
  }

  public static class Factory implements MapModuleFactory<RageModule> {
    @Override
    public RageModule parse(MapFactory factory, Logger logger, Document doc)
        throws InvalidXMLException {
      if (doc.getRootElement().getChild("rage") != null) {
        return new RageModule();
      } else {
        return null;
      }
    }
  }
}
