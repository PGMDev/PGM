package tc.oc.pgm.api.region;

import org.bukkit.World;
import tc.oc.pgm.api.filter.FilterDefinition;
import tc.oc.pgm.api.match.Match;

/** A {@link tc.oc.pgm.api.region.Region} that is not a reference */
public interface RegionDefinition extends FilterDefinition, Region {
  interface Static extends RegionDefinition, Region.Static {}

  interface HardStatic extends RegionDefinition.Static {
    @Override
    default boolean isStatic() {
      return true;
    }

    @Override
    default RegionDefinition.Static getStatic(Match match) {
      return this;
    }

    @Override
    default RegionDefinition.Static getStatic(World world) {
      return this;
    }

    @Override
    default RegionDefinition.Static getStaticImpl(Match match) {
      return this;
    }
  }
}
