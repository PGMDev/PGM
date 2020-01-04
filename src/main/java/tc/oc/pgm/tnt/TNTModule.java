package tc.oc.pgm.tnt;

import java.util.Set;
import java.util.logging.Logger;
import org.jdom2.Document;
import org.jdom2.Element;
import org.joda.time.Duration;
import tc.oc.component.Component;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.filters.CauseFilter;
import tc.oc.pgm.filters.DenyFilter;
import tc.oc.pgm.map.MapModule;
import tc.oc.pgm.map.MapModuleContext;
import tc.oc.pgm.maptag.MapTag;
import tc.oc.pgm.module.ModuleDescription;
import tc.oc.pgm.regions.EverywhereRegion;
import tc.oc.pgm.regions.RFAScope;
import tc.oc.pgm.regions.RegionFilterApplication;
import tc.oc.pgm.regions.RegionModule;
import tc.oc.pgm.util.XMLUtils;
import tc.oc.xml.InvalidXMLException;

@ModuleDescription(name = "TNT", depends = RegionModule.class)
public class TNTModule extends MapModule<TNTMatchModule> {
  public static final int DEFAULT_DISPENSER_NUKE_LIMIT = 16;
  public static final float DEFAULT_DISPENSER_NUKE_MULTIPLIER = 0.25f;

  private static final MapTag AUTOTNT_TAG = MapTag.forName("autotnt");

  private final TNTProperties properties;

  public TNTModule(TNTProperties properties) {
    this.properties = properties;
  }

  @Override
  public void loadTags(Set<MapTag> tags) {
    if (properties.instantIgnite) tags.add(AUTOTNT_TAG);
  }

  @Override
  public TNTMatchModule createMatchModule(Match match) {
    return new TNTMatchModule(match, this.properties);
  }

  // ---------------------
  // ---- XML Parsing ----
  // ---------------------

  public static TNTModule parse(MapModuleContext context, Logger logger, Document doc)
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
      context
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
