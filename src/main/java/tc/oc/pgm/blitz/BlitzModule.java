package tc.oc.pgm.blitz;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.List;
import java.util.logging.Logger;
import org.jdom2.Document;
import org.jdom2.Element;
import tc.oc.pgm.api.map.MapContext;
import tc.oc.pgm.api.map.MapModule;
import tc.oc.pgm.api.map.factory.MapModuleFactory;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.util.XMLUtils;
import tc.oc.xml.InvalidXMLException;
import tc.oc.xml.Node;

public class BlitzModule implements MapModule {
  final BlitzConfig config;

  public BlitzModule(BlitzConfig config) {
    this.config = checkNotNull(config);
  }

  // FIXME: custom scoreboard
  /*@Override
  public Component getGame(MapContext context) {
    if (isDisabled(context)) return null;
    if (context.hasModule(TeamModule.class)) {
      return new PersonalizedTranslatable("match.scoreboard.playersRemaining.title");
    } else if (context.hasModule(FreeForAllModule.class) && config.getNumLives() > 1) {
      return new PersonalizedTranslatable("match.scoreboard.livesRemaining.title");
    } else {
      return new PersonalizedTranslatable("match.scoreboard.blitz.title");
    }
  }*/

  @Override
  public BlitzMatchModule createMatchModule(Match match) {
    return new BlitzMatchModule(match, this.config);
  }

  public static class Factory implements MapModuleFactory<BlitzModule> {
    @Override
    public BlitzModule parse(MapContext context, Logger logger, Document doc)
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
}
