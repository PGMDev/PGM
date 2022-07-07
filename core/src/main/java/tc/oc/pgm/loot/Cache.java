package tc.oc.pgm.loot;

import tc.oc.pgm.api.filter.Filter;
import tc.oc.pgm.api.region.Region;

public class Cache {
  /*
  A storage of items from chests in defined regions, if it passes the filter, it will be cached.
  When the chest is refilled, it will be populated with items in the cache instead of from <loot>, <maybe>, etc.
   */
  private final Filter filter;
  private final Region region;

  public Cache(Filter filter, Region region) {
    this.filter = filter;
    this.region = region;
  }

  public Filter getFilter() {
    return filter;
  }

  public Region getRegion() {
    return region;
  }
}
