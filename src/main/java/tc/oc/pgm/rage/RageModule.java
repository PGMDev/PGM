package tc.oc.pgm.rage;

import com.google.common.collect.ImmutableList;
import java.util.Collection;
import java.util.logging.Logger;
import org.jdom2.Document;
import tc.oc.component.Component;
import tc.oc.component.types.PersonalizedTranslatable;
import tc.oc.pgm.api.map.MapContext;
import tc.oc.pgm.api.map.MapModule;
import tc.oc.pgm.api.map.factory.MapModuleFactory;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.match.MatchModule;
import tc.oc.pgm.blitz.BlitzModule;
import tc.oc.xml.InvalidXMLException;

public class RageModule implements MapModule {

  private final boolean blitz;

  public RageModule(boolean blitz) {
    this.blitz = blitz;
  }

  private static final Component GAME = new PersonalizedTranslatable("match.scoreboard.rage.title");

  // FIXME: custom sidebar
  /*@Override
  public Component getGame(MapContext context) {
    return blitz ? GAME : null;
  }*/

  @Override
  public MatchModule createMatchModule(Match match) {
    return new RageMatchModule(match);
  }

  public static class Factory implements MapModuleFactory<RageModule> {
    @Override
    public Collection<Class<? extends MapModule>> getWeakDependencies() {
      return ImmutableList.of(BlitzModule.class);
    }

    @Override
    public RageModule parse(MapContext context, Logger logger, Document doc)
        throws InvalidXMLException {
      if (doc.getRootElement().getChild("rage") != null) {
        return new RageModule(context.hasModule(BlitzModule.class));
      } else {
        return null;
      }
    }
  }
}
