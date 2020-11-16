package tc.oc.pgm.blitz;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Range;
import java.util.Collection;
import java.util.List;
import java.util.logging.Logger;
import org.jdom2.Document;
import org.jdom2.Element;
import tc.oc.pgm.api.map.MapModule;
import tc.oc.pgm.api.map.MapTag;
import tc.oc.pgm.api.map.factory.MapFactory;
import tc.oc.pgm.api.map.factory.MapModuleFactory;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.util.xml.InvalidXMLException;
import tc.oc.pgm.util.xml.Node;
import tc.oc.pgm.util.xml.XMLUtils;

public class BlitzModule implements MapModule {

  private static final Collection<MapTag> TAGS =
      ImmutableList.of(MapTag.create("blitz", "Blitz", true, true));
  private final BlitzConfig config;

  public BlitzModule(BlitzConfig config) {
    this.config = checkNotNull(config);
  }

  @Override
  public BlitzMatchModule createMatchModule(Match match) {
    return new BlitzMatchModule(match, config);
  }

  @Override
  public Collection<MapTag> getTags() {
    return TAGS;
  }

  public static class Factory implements MapModuleFactory<BlitzModule> {
    @Override
    public BlitzModule parse(MapFactory factory, Logger logger, Document doc)
        throws InvalidXMLException {
      List<Element> blitzElements = doc.getRootElement().getChildren("blitz");
      BlitzConfig config = new BlitzConfig(Integer.MAX_VALUE, false);

      for (Element blitzEl : blitzElements) {
        boolean broadcastLives = XMLUtils.parseBoolean(blitzEl.getChild("broadcastLives"), true);
        int lives =
            XMLUtils.parseNumberInRange(
                Node.fromChildOrAttr(blitzEl, "lives"), Integer.class, Range.atLeast(1), 1);

        config = new BlitzConfig(lives, broadcastLives);
      }

      if (config.lives != Integer.MAX_VALUE) {
        return new BlitzModule(config);
      }

      return null;
    }
  }
}
