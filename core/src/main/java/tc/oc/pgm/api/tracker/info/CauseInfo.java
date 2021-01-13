package tc.oc.pgm.api.tracker.info;

import org.checkerframework.checker.nullness.qual.Nullable;

public interface CauseInfo extends TrackerInfo {
  @Nullable
  TrackerInfo getCause();
}
