package tc.oc.pgm.enderchest;

import lombok.Getter;
import tc.oc.pgm.api.filter.Filter;
import tc.oc.pgm.api.region.Region;

@Getter
public class Dropoff {

  private final Region region;
  private final Filter filter;

  public Dropoff(Region region, Filter filter) {
    this.region = region;
    this.filter = filter;
  }
}
