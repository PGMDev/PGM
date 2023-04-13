package tc.oc.pgm.blitz;

import static tc.oc.pgm.util.Assert.assertNotNull;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Range;
import java.util.Collection;
import java.util.List;
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
import tc.oc.pgm.filters.parse.DynamicFilterValidation;
import tc.oc.pgm.filters.parse.FilterParser;
import tc.oc.pgm.util.xml.InvalidXMLException;
import tc.oc.pgm.util.xml.Node;
import tc.oc.pgm.util.xml.XMLUtils;

public class BlitzModule implements MapModule<BlitzMatchModule> {

  private static final Collection<MapTag> TAGS =
      ImmutableList.of(new MapTag("blitz", "Blitz", true, true));
  private final BlitzConfig config;

  public BlitzModule(BlitzConfig config) {
    this.config = assertNotNull(config);
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
    public Collection<Class<? extends MapModule<?>>> getWeakDependencies() {
      return ImmutableList.of(FilterModule.class);
    }

    @Override
    public BlitzModule parse(MapFactory factory, Logger logger, Document doc)
        throws InvalidXMLException {
      List<Element> blitzElements = doc.getRootElement().getChildren("blitz");

      int lives = Integer.MAX_VALUE;
      boolean broadcastLives = false;
      Filter filter = null;
      Filter scoreboardFilter = null;
      Filter joinFilter = null;

      FilterParser filters = factory.getFilters();
      for (Element blitzEl : blitzElements) {
        broadcastLives = XMLUtils.parseBoolean(blitzEl.getChild("broadcastLives"), true);
        lives =
            XMLUtils.parseNumberInRange(
                Node.fromChildOrAttr(blitzEl, "lives"), Integer.class, Range.atLeast(1), 1);
        filter = filters.parseProperty(blitzEl, "filter", StaticFilter.ALLOW);
        scoreboardFilter =
            filters.parseProperty(
                blitzEl, "scoreboard-filter", StaticFilter.ALLOW, DynamicFilterValidation.PARTY);
        joinFilter = filters.parseProperty(blitzEl, "join-filter", StaticFilter.DENY);
      }

      if (lives != Integer.MAX_VALUE) {
        return new BlitzModule(
            new BlitzConfig(lives, broadcastLives, filter, scoreboardFilter, joinFilter));
      }

      return null;
    }
  }
}
