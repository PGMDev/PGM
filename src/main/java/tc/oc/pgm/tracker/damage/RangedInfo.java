package tc.oc.pgm.tracker.damage;

import javax.annotation.Nullable;
import org.bukkit.Location;

public interface RangedInfo extends TrackerInfo {
  @Nullable
  Location getOrigin();
}
