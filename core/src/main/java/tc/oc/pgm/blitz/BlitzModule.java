package tc.oc.pgm.blitz;

import static tc.oc.pgm.util.Assert.assertNotNull;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Range;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;
import org.jdom2.Document;
import org.jdom2.Element;
import tc.oc.pgm.api.filter.Filter;
import tc.oc.pgm.api.map.MapModule;
import tc.oc.pgm.api.map.MapTag;
import tc.oc.pgm.api.map.factory.MapFactory;
import tc.oc.pgm.api.map.factory.MapModuleFactory;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.filters.FilterModule;
import tc.oc.pgm.filters.matcher.StaticFilter;
import tc.oc.pgm.filters.parse.FilterParser;
import tc.oc.pgm.util.xml.InvalidXMLException;
import tc.oc.pgm.util.xml.Node;
import tc.oc.pgm.util.xml.XMLUtils;

public class BlitzModule implements MapModule {

  private static final MapTag BLITZ = new MapTag("blitz", "Blitz", true, true);
  // Sets Blitz into an internal MapTag that doesn't display on scoreboard
  private static final MapTag BLITZ_IDENTIFIER = new MapTag("blitz", "Blitz", false, true);
  private static final MapTag BLITZ_RAGE =
      new MapTag("br", "blitz-rage", "Blitz: Rage", true, true);
  private final BlitzConfig config;
  private final Collection<MapTag> tags;

  public BlitzModule(BlitzConfig config, Collection<MapTag> tags) {
    this.config = assertNotNull(config);
    this.tags = tags;
  }

  @Override
  public BlitzMatchModule createMatchModule(Match match) {
    return new BlitzMatchModule(match, config);
  }

  @Override
  public Collection<MapTag> getTags() {
    return tags;
  }

  public static class Factory implements MapModuleFactory<BlitzModule> {

    @Override
    public Collection<Class<? extends MapModule>> getWeakDependencies() {
      return ImmutableList.of(FilterModule.class);
    }

    @Override
    public BlitzModule parse(MapFactory factory, Logger logger, Document doc)
        throws InvalidXMLException {
      List<Element> blitzElements = doc.getRootElement().getChildren("blitz");

      int lives = Integer.MAX_VALUE;
      boolean broadcastLives = false;
      Filter filter = null;
      Filter joinFilter = null;
      Set<MapTag> tags = new HashSet<>();

      FilterParser filters = factory.getFilters();
      for (Element blitzEl : blitzElements) {
        broadcastLives = XMLUtils.parseBoolean(blitzEl.getChild("broadcastLives"), true);
        lives =
            XMLUtils.parseNumberInRange(
                Node.fromChildOrAttr(blitzEl, "lives"), Integer.class, Range.atLeast(1), 1);
        filter = filters.parseProperty(blitzEl, "filter", StaticFilter.ALLOW);
        joinFilter = filters.parseProperty(blitzEl, "join-filter", StaticFilter.DENY);

        if (doc.getRootElement().getChildren("rage") != null) {
          // Map will be tagged #blitz-rage, #blitz, and #rage
          // Will display "Blitz: Rage" rather than "Blitz and Rage" on scoreboard
          tags.add(BLITZ_RAGE);
          tags.add(BLITZ_IDENTIFIER);
        } else {
          tags.add(BLITZ);
        }
      }

      if (lives != Integer.MAX_VALUE) {
        return new BlitzModule(new BlitzConfig(lives, broadcastLives, filter, joinFilter), tags);
      }

      return null;
    }
  }
}
