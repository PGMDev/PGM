package tc.oc.pgm.api.tracker.info;

import javax.annotation.Nullable;

public interface CauseInfo extends TrackerInfo {
  @Nullable
  TrackerInfo getCause();
}
