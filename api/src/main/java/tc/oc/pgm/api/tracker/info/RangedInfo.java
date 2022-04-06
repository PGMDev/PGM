package tc.oc.pgm.api.tracker.info;

import javax.annotation.Nullable;
import org.bukkit.Location;

public interface RangedInfo extends TrackerInfo {
  @Nullable
  Location getOrigin();
}
