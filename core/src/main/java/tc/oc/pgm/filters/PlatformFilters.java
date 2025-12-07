package tc.oc.pgm.filters;

import tc.oc.pgm.api.filter.Filter;
import tc.oc.pgm.util.platform.Platform;

public interface PlatformFilters {
  PlatformFilters PLATFORM_FILTERS = Platform.get(PlatformFilters.class);

  Filter gliding();
}
