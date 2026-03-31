package tc.oc.pgm.api.tracker.info;

import org.bukkit.Location;
import org.jspecify.annotations.Nullable;

public interface RangedInfo extends TrackerInfo {
  @Nullable
  Location origin();

  @Deprecated
  default @Nullable Location getOrigin() {
    return origin();
  }
}
