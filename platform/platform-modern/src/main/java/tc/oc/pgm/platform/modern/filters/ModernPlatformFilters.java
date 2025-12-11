package tc.oc.pgm.platform.modern.filters;

import static tc.oc.pgm.util.platform.Supports.Variant.PAPER;

import tc.oc.pgm.api.filter.Filter;
import tc.oc.pgm.filters.PlatformFilters;
import tc.oc.pgm.util.platform.Supports;

@Supports(PAPER)
public class ModernPlatformFilters implements PlatformFilters {
  @Override
  public Filter gliding() {
    return GlidingFilter.INSTANCE;
  }

  @Override
  public Filter riptiding() {
    return RiptidingFilter.INSTANCE;
  }

  @Override
  public Filter crawling() {
    return CrawlingFilter.INSTANCE;
  }
}
