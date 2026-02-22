package tc.oc.pgm.tnt;

import com.google.common.collect.Range;
import java.time.Duration;
import java.util.Collection;
import java.util.List;
import java.util.logging.Logger;
import net.kyori.adventure.text.Component;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import tc.oc.pgm.api.map.MapModule;
import tc.oc.pgm.api.map.MapTag;
import tc.oc.pgm.api.map.factory.MapFactory;
import tc.oc.pgm.api.map.factory.MapModuleFactory;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.filters.matcher.CauseFilter;
import tc.oc.pgm.filters.operator.DenyFilter;
import tc.oc.pgm.regions.EverywhereRegion;
import tc.oc.pgm.regions.RFAScope;
import tc.oc.pgm.regions.RegionFilterApplication;
import tc.oc.pgm.regions.RegionModule;
import tc.oc.pgm.util.xml.InvalidXMLException;

@NullMarked
public class TNTModule implements MapModule<TNTMatchModule> {
  private static final Collection<MapTag> TAGS = List.of(new MapTag("autotnt", "Instant TNT"));
  public static final int DEFAULT_DISPENSER_NUKE_LIMIT = 16;
  public static final float DEFAULT_DISPENSER_NUKE_MULTIPLIER = 0.25f;

  private final TNTProperties properties;

  public TNTModule(TNTProperties properties) {
    this.properties = properties;
  }

  @Override
  public Collection<MapTag> getTags() {
    return properties.instantIgnite() ? TAGS : List.of();
  }

  @Override
  public TNTMatchModule createMatchModule(Match match) {
    return new TNTMatchModule(match, this.properties);
  }

  public static class Factory implements MapModuleFactory<TNTModule> {

    public static final Range<Duration> FUSE = Range.closed(Duration.ZERO, Duration.ofSeconds(4));

    @Override
    public Collection<Class<? extends MapModule<?>>> getSoftDependencies() {
      return List.of(RegionModule.class);
    }

    @Override
    public @Nullable TNTModule parse(MapFactory factory, Logger logger, Document doc)
        throws InvalidXMLException {
      var parser = factory.getParser();
      Float yield = null;
      Float power = null;
      boolean instantIgnite = false;
      boolean blockDamage = true;
      Duration fuse = null;
      int dispenserNukeLimit = DEFAULT_DISPENSER_NUKE_LIMIT;
      float dispenserNukeMultiplier = DEFAULT_DISPENSER_NUKE_MULTIPLIER;
      boolean licensing = true;
      boolean friendlyDefuse = true;
      boolean exists = false;

      for (Element tntElement : doc.getRootElement().getChildren("tnt")) {
        exists = true;
        instantIgnite =
            parser.parseBool(tntElement, "instant-ignite", "instantignite").optional(instantIgnite);
        blockDamage =
            parser.parseBool(tntElement, "block-damage", "blockdamage").optional(blockDamage);
        yield = parser.parseFloat(tntElement, "yield").optional(yield);
        power = parser.parseFloat(tntElement, "power").optional(power);
        dispenserNukeLimit =
            parser.parseInt(tntElement, "dispenser-tnt-limit").optional(dispenserNukeLimit);
        dispenserNukeMultiplier = parser
            .parseFloat(tntElement, "dispenser-tnt-multiplier")
            .optional(dispenserNukeMultiplier);
        licensing = parser.parseBool(tntElement, "licensing").optional(licensing);
        friendlyDefuse = parser.parseBool(tntElement, "friendly-defuse").optional(friendlyDefuse);
        // TNT disappears on the client after 4 seconds, no way to extend it
        // If this is ever really needed, we could spawn new entities on the client every 4s
        fuse = parser.duration(tntElement, "fuse").between(FUSE).optional(fuse);
      }

      if (!blockDamage) {
        factory
            .needModule(RegionModule.class)
            .getRFAContextBuilder()
            .prepend(new RegionFilterApplication(
                RFAScope.BLOCK_BREAK,
                EverywhereRegion.INSTANCE,
                new DenyFilter(new CauseFilter(CauseFilter.Cause.EXPLOSION)),
                (Component) null,
                false));
      }

      return exists
          ? new TNTModule(new TNTProperties(
              yield,
              power,
              instantIgnite,
              blockDamage,
              fuse,
              dispenserNukeLimit,
              dispenserNukeMultiplier,
              licensing,
              friendlyDefuse))
          : null;
    }
  }
}
