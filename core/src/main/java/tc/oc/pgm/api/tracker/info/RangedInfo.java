package tc.oc.pgm.api.tracker.info;

import org.jetbrains.annotations.Nullable;
import org.bukkit.Location;

public interface RangedInfo extends TrackerInfo {
  @Nullable
  Location getOrigin();
}
