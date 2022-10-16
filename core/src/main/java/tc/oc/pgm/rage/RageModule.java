package tc.oc.pgm.rage;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Logger;
import org.jdom2.Document;
import tc.oc.pgm.api.map.MapModule;
import tc.oc.pgm.api.map.MapTag;
import tc.oc.pgm.api.map.factory.MapFactory;
import tc.oc.pgm.api.map.factory.MapModuleFactory;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.match.MatchModule;
import tc.oc.pgm.util.xml.InvalidXMLException;

public class RageModule implements MapModule {
  private static final MapTag RAGE = new MapTag("rage", "Rage", true, true);
  // Sets Rage into an internal MapTag that doesn't display on scoreboard
  private static final MapTag RAGE_IDENTIFIER = new MapTag("rage", "Rage", false, true);
  private final Collection<MapTag> tags;

  public RageModule(Collection<MapTag> tags) {
    this.tags = tags;
  }

  @Override
  public MatchModule createMatchModule(Match match) {
    return new RageMatchModule(match);
  }

  @Override
  public Collection<MapTag> getTags() {
    return tags;
  }

  public static class Factory implements MapModuleFactory<RageModule> {
    @Override
    public RageModule parse(MapFactory factory, Logger logger, Document doc)
        throws InvalidXMLException {
      Set<MapTag> tags = new HashSet<>();
      if (doc.getRootElement().getChild("rage") != null) {
        // If the blitz module is loaded with Rage, display "Blitz: Rage" on scoreboard instead of
        // "Blitz and Rage"
        if (doc.getRootElement().getChild("blitz") != null) {
          tags.add(RAGE_IDENTIFIER);
        } else {
          tags.add(RAGE);
        }
        return new RageModule(tags);
      } else {
        return null;
      }
    }
  }
}
