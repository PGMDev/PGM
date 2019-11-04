package tc.oc.pgm.blitz;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.List;
import java.util.logging.Logger;
import org.jdom2.Document;
import org.jdom2.Element;
import tc.oc.component.Component;
import tc.oc.component.types.PersonalizedTranslatable;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.ffa.FreeForAllModule;
import tc.oc.pgm.map.MapModule;
import tc.oc.pgm.map.MapModuleContext;
import tc.oc.pgm.match.MatchModule;
import tc.oc.pgm.module.ModuleDescription;
import tc.oc.pgm.teams.TeamModule;
import tc.oc.pgm.util.XMLUtils;
import tc.oc.xml.InvalidXMLException;
import tc.oc.xml.Node;

@ModuleDescription(name = "Blitz")
public class BlitzModule extends MapModule {
  final BlitzConfig config;

  public BlitzModule(BlitzConfig config) {
    this.config = checkNotNull(config);
  }

  @Override
  public Component getGame(MapModuleContext context) {
    if (isDisabled(context)) return null;
    if (context.hasModule(TeamModule.class)) {
      return new PersonalizedTranslatable("match.scoreboard.playersRemaining.title");
    } else if (context.hasModule(FreeForAllModule.class) && config.getNumLives() > 1) {
      return new PersonalizedTranslatable("match.scoreboard.livesRemaining.title");
    } else {
      return new PersonalizedTranslatable("match.scoreboard.blitz.title");
    }
  }

  @Override
  public MatchModule createMatchModule(Match match) {
    return new BlitzMatchModule(match, this.config);
  }

  public boolean isDisabled(MapModuleContext context) {
    return config.lives == Integer.MAX_VALUE;
  }

  // ---------------------
  // ---- XML Parsing ----
  // ---------------------

  public static BlitzModule parse(MapModuleContext context, Logger logger, Document doc)
      throws InvalidXMLException {
    List<Element> blitzElements = doc.getRootElement().getChildren("blitz");
    BlitzConfig config = new BlitzConfig(Integer.MAX_VALUE, false);

    for (Element blitzEl : blitzElements) {
      boolean broadcastLives = XMLUtils.parseBoolean(blitzEl.getChild("broadcastLives"), true);
      int lives = XMLUtils.parseNumber(Node.fromChildOrAttr(blitzEl, "lives"), Integer.class, 1);
      config = new BlitzConfig(lives, broadcastLives);
    }

    return new BlitzModule(config);
  }
}
