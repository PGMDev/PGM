package tc.oc.pgm.platform.sportpaper.filters;

import static tc.oc.pgm.util.platform.Supports.Variant.SPORTPAPER;

import tc.oc.pgm.api.filter.Filter;
import tc.oc.pgm.filters.PlatformFilters;
import tc.oc.pgm.filters.matcher.StaticFilter;
import tc.oc.pgm.util.platform.Supports;

@Supports(SPORTPAPER)
public class SpPlatformFilters implements PlatformFilters {
  @Override
  public Filter gliding() {
    // Gliding is not possible in 1.8
    return StaticFilter.DENY;
  }

  @Override
  public Filter riptiding() {
    // Riptiding is not possible in 1.8
    return StaticFilter.DENY;
  }
}
