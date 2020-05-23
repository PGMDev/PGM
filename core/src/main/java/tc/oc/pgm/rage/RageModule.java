package tc.oc.pgm.rage;

import com.google.common.collect.ImmutableList;
import java.util.Collection;
import java.util.logging.Logger;
import org.jdom2.Document;
import org.jdom2.Element;
import tc.oc.pgm.api.map.MapModule;
import tc.oc.pgm.api.map.MapTag;
import tc.oc.pgm.api.map.factory.MapFactory;
import tc.oc.pgm.api.map.factory.MapModuleFactory;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.match.MatchModule;
import tc.oc.pgm.util.xml.InvalidXMLException;
import tc.oc.pgm.util.xml.XMLUtils;

public class RageModule implements MapModule {
  private static final Collection<MapTag> TAGS =
      ImmutableList.of(MapTag.create("rage", "Rage", true, true));

  private final boolean allProjectiles;

  public RageModule(boolean allProjectiles) {
    this.allProjectiles = allProjectiles;
  }

  @Override
  public MatchModule createMatchModule(Match match) {
    return new RageMatchModule(match, this.allProjectiles);
  }

  @Override
  public Collection<MapTag> getTags() {
    return TAGS;
  }

  public static class Factory implements MapModuleFactory<RageModule> {
    @Override
    public RageModule parse(MapFactory factory, Logger logger, Document doc)
        throws InvalidXMLException {
      Element rageEle = doc.getRootElement().getChild("rage");
      if (rageEle != null) {
        boolean allProjectiles =
            XMLUtils.parseBoolean(rageEle.getAttribute("all-projectiles"), false);
        return new RageModule(allProjectiles);
      } else {
        return null;
      }
    }
  }
}
