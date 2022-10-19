package tc.oc.pgm.api.tracker.info;

import org.jetbrains.annotations.Nullable;

public interface CauseInfo extends TrackerInfo {
  @Nullable
  TrackerInfo getCause();
}
