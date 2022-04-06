package tc.oc.pgm.blitz;

import static com.google.common.base.Preconditions.checkNotNull;

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
import tc.oc.pgm.api.xml.InvalidXMLException;
import tc.oc.pgm.api.xml.Node;
import tc.oc.pgm.filters.FilterModule;
import tc.oc.pgm.filters.StaticFilter;
import tc.oc.pgm.util.xml.XMLUtils;

public class BlitzModule implements MapModule {

  private static final Collection<MapTag> TAGS =
      ImmutableList.of(new MapTag("blitz", "Blitz", true, true));
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

      for (Element blitzEl : blitzElements) {
        broadcastLives = XMLUtils.parseBoolean(blitzEl.getChild("broadcastLives"), true);
        lives =
            XMLUtils.parseNumberInRange(
                Node.fromChildOrAttr(blitzEl, "lives"), Integer.class, Range.atLeast(1), 1);
        filter = factory.getFilters().parseFilterProperty(blitzEl, "filter", StaticFilter.ALLOW);
      }

      if (lives != Integer.MAX_VALUE) {
        return new BlitzModule(new BlitzConfig(lives, broadcastLives, filter));
      }

      return null;
    }
  }
}
