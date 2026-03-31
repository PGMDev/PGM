package tc.oc.pgm.api.tracker.info;

import org.jspecify.annotations.Nullable;

public interface CauseInfo extends TrackerInfo {
  @Nullable
  TrackerInfo cause();

  @Deprecated
  default @Nullable TrackerInfo getCause() {
    return cause();
  }
}
