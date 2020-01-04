package tc.oc.pgm.rage;

import java.util.logging.Logger;
import org.jdom2.Document;
import tc.oc.component.Component;
import tc.oc.component.types.PersonalizedTranslatable;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.blitz.BlitzModule;
import tc.oc.pgm.map.MapModule;
import tc.oc.pgm.map.MapModuleContext;
import tc.oc.pgm.maptag.MapTag;
import tc.oc.pgm.maptag.MapTagSet;
import tc.oc.pgm.match.MatchModule;
import tc.oc.pgm.module.ModuleDescription;

@ModuleDescription(
    name = "Rage",
    follows = {BlitzModule.class})
public class RageModule extends MapModule {

  private static final MapTag RAGE_TAG = MapTag.forName("rage");

  private final boolean blitz;

  public RageModule(boolean blitz) {
    this.blitz = blitz;
  }

  private static final Component GAME = new PersonalizedTranslatable("match.scoreboard.rage.title");

  @Override
  public Component getGame(MapModuleContext context) {
    return blitz ? GAME : null;
  }

  @Override
  public void loadTags(MapTagSet tags) {
    tags.add(RAGE_TAG);
  }

  @Override
  public MatchModule createMatchModule(Match match) {
    return new RageMatchModule(match);
  }

  // ---------------------
  // ---- XML Parsing ----
  // ---------------------

  public static RageModule parse(MapModuleContext context, Logger logger, Document doc) {

    if (doc.getRootElement().getChild("rage") != null) {
      return new RageModule(context.hasModule(BlitzModule.class));
    } else {
      return null;
    }
  }
}
