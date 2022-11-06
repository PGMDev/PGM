package tc.oc.pgm.loot;

import tc.oc.pgm.api.feature.FeatureDefinition;
import tc.oc.pgm.api.feature.FeatureInfo;
import tc.oc.pgm.api.filter.Filter;
import tc.oc.pgm.api.region.Region;
import tc.oc.pgm.filters.operator.AllFilter;

@FeatureInfo(name = "cache")
public class Cache implements FeatureDefinition {

  private final Region region;
  private final Filter filter;
  private final Filter jointFilter;

  public Cache(Region region, Filter filter) {
    this.region = region;
    this.filter = filter;
    this.jointFilter = AllFilter.of(region, filter);
  }

  public Region region() {
    return this.region;
  }

  public Filter filter() {
    return this.filter;
  }

  public Filter jointFilter() {
    return this.jointFilter;
  }
}
