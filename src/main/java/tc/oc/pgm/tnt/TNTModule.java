package tc.oc.pgm.tnt;

import com.google.common.collect.ImmutableList;
import java.util.Collection;
import java.util.Collections;
import java.util.logging.Logger;
import org.jdom2.Document;
import org.jdom2.Element;
import org.joda.time.Duration;
import tc.oc.component.Component;
import tc.oc.pgm.api.map.MapModule;
import tc.oc.pgm.api.map.MapTag;
import tc.oc.pgm.api.map.factory.MapFactory;
import tc.oc.pgm.api.map.factory.MapModuleFactory;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.match.MatchModule;
import tc.oc.pgm.filters.CauseFilter;
import tc.oc.pgm.filters.DenyFilter;
import tc.oc.pgm.regions.EverywhereRegion;
import tc.oc.pgm.regions.RFAScope;
import tc.oc.pgm.regions.RegionFilterApplication;
import tc.oc.pgm.regions.RegionModule;
import tc.oc.pgm.util.XMLUtils;
import tc.oc.xml.InvalidXMLException;

public class TNTModule implements MapModule {
  private static final Collection<MapTag> TAGS =
      ImmutableList.of(MapTag.create("autotnt", "Instant TNT", false, true));
  public static final int DEFAULT_DISPENSER_NUKE_LIMIT = 16;
  public static final float DEFAULT_DISPENSER_NUKE_MULTIPLIER = 0.25f;

  private final TNTProperties properties;

  public TNTModule(TNTProperties properties) {
    this.properties = properties;
  }

  @Override
  public Collection<MapTag> getTags() {
    return properties.instantIgnite ? TAGS : Collections.emptyList();
  }

  @Override
  public MatchModule createMatchModule(Match match) {
    return new TNTMatchModule(match, this.properties);
  }

  public static class Factory implements MapModuleFactory<TNTModule> {
    @Override
    public Collection<Class<? extends MapModule>> getSoftDependencies() {
      return ImmutableList.of(RegionModule.class);
    }

    @Override
    public TNTModule parse(MapFactory factory, Logger logger, Document doc)
        throws InvalidXMLException {
      Float yield = null;
      Float power = null;
      boolean instantIgnite = false;
      boolean blockDamage = true;
      Duration fuse = null;
      int dispenserNukeLimit = DEFAULT_DISPENSER_NUKE_LIMIT;
      float dispenserNukeMultiplier = DEFAULT_DISPENSER_NUKE_MULTIPLIER;
      boolean licensing = true;
      boolean friendlyDefuse = true;

      for (Element tntElement : doc.getRootElement().getChildren("tnt")) {
        instantIgnite =
            XMLUtils.parseBoolean(
                XMLUtils.getUniqueChild(tntElement, "instantignite"), instantIgnite);
        blockDamage =
            XMLUtils.parseBoolean(XMLUtils.getUniqueChild(tntElement, "blockdamage"), blockDamage);
        yield =
            XMLUtils.parseNumber(XMLUtils.getUniqueChild(tntElement, "yield"), Float.class, yield);
        power =
            XMLUtils.parseNumber(XMLUtils.getUniqueChild(tntElement, "power"), Float.class, power);
        dispenserNukeLimit =
            XMLUtils.parseNumber(
                XMLUtils.getUniqueChild(tntElement, "dispenser-tnt-limit"),
                Integer.class,
                dispenserNukeLimit);
        dispenserNukeMultiplier =
            XMLUtils.parseNumber(
                XMLUtils.getUniqueChild(tntElement, "dispenser-tnt-multiplier"),
                Float.class,
                dispenserNukeMultiplier);
        licensing =
            XMLUtils.parseBoolean(XMLUtils.getUniqueChild(tntElement, "licensing"), licensing);
        friendlyDefuse =
            XMLUtils.parseBoolean(
                XMLUtils.getUniqueChild(tntElement, "friendly-defuse"), friendlyDefuse);

        Element fuseElement = XMLUtils.getUniqueChild(tntElement, "fuse");
        if (fuseElement != null) {
          fuse = XMLUtils.parseDuration(fuseElement, fuse);

          if (fuse.isLongerThan(Duration.standardSeconds(4))) {
            // TNT disappears on the client after 4 seconds, no way to extend it
            // If this is ever really needed, we could spawn new entities on the client every 4
            // seconds
            throw new InvalidXMLException("TNT fuse cannot be longer than 4 seconds", fuseElement);
          }
        }
      }

      if (!blockDamage) {
        factory
            .needModule(RegionModule.class)
            .getRFAContext()
            .prepend(
                new RegionFilterApplication(
                    RFAScope.BLOCK_BREAK,
                    EverywhereRegion.INSTANCE,
                    new DenyFilter(new CauseFilter(CauseFilter.Cause.EXPLOSION)),
                    (Component) null,
                    false));
      }

      return new TNTModule(
          new TNTProperties(
              yield,
              power,
              instantIgnite,
              blockDamage,
              fuse,
              dispenserNukeLimit,
              dispenserNukeMultiplier,
              licensing,
              friendlyDefuse));
    }
  }
}
