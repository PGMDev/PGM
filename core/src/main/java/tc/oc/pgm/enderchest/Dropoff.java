package tc.oc.pgm.enderchest;

import tc.oc.pgm.api.filter.Filter;
import tc.oc.pgm.api.region.Region;

public record Dropoff(Region region, Filter filter) {
  @Deprecated
  public Region getRegion() {
    return region();
  }

  @Deprecated
  public Filter getFilter() {
    return filter();
  }
}
