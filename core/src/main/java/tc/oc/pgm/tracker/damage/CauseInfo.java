package tc.oc.pgm.tracker.damage;

import javax.annotation.Nullable;

public interface CauseInfo extends TrackerInfo {
  @Nullable
  TrackerInfo getCause();
}
