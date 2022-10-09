package tc.oc.pgm.api.tracker.info;

import org.bukkit.Location;
import org.jetbrains.annotations.Nullable;

public interface RangedInfo extends TrackerInfo {
  @Nullable
  Location getOrigin();
}
