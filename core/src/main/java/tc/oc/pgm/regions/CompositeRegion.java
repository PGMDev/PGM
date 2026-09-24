package tc.oc.pgm.regions;

import java.util.Arrays;
import java.util.function.Predicate;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.region.Region;
import tc.oc.pgm.api.region.RegionDefinition;

/** A region defined by combining an array of child regions. */
public abstract class CompositeRegion implements RegionDefinition.Static {
  protected final Region[] regions;

  protected CompositeRegion(Region... regions) {
    this.regions = regions;
  }

  public Region[] getRegions() {
    return this.regions;
  }

  protected abstract CompositeRegion rebuild(Region[] regions);

  // contains() deliberately keeps its own loop, it is hot while these are parse-time only.
  protected boolean all(Predicate<Region> test) {
    for (Region region : this.regions) {
      if (!test.test(region)) return false;
    }
    return true;
  }

  protected boolean any(Predicate<Region> test) {
    for (Region region : this.regions) {
      if (test.test(region)) return true;
    }
    return false;
  }

  @Override
  public boolean isStatic() {
    return all(Region::isStatic);
  }

  @Override
  public Region.Static getStaticImpl(Match match) {
    Region[] statics = new Region[this.regions.length];
    for (int i = 0; i < this.regions.length; i++) {
      statics[i] = this.regions[i].getStatic(match);
    }
    return rebuild(statics);
  }

  @Override
  public String toString() {
    return getClass().getSimpleName() + "{regions=" + Arrays.toString(this.regions) + "}";
  }
}
