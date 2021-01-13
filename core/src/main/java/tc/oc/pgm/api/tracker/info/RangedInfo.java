package tc.oc.pgm.api.tracker.info;

import org.bukkit.Location;
import org.checkerframework.checker.nullness.qual.Nullable;

public interface RangedInfo extends TrackerInfo {
  @Nullable
  Location getOrigin();
}
