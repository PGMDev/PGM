package tc.oc.pgm.regions;

import java.util.ArrayList;
import java.util.List;
import org.bukkit.util.Vector;
import tc.oc.pgm.api.region.Region;
import tc.oc.pgm.util.collection.ContextStore;

/**
 * Class that manages many named regions.
 *
 * <p>The RegionManager correlates regions with names so they can be looked up and resolved at a
 * later time.
 */
public class RegionContext extends ContextStore<Region> {
  /**
   * Gets all regions that contain the given point.
   *
   * @param point Point to check against.
   * @return Regions where region.contains(point) == true
   */
  public List<Region> getContaining(Vector point) {
    List<Region> result = new ArrayList<Region>();
    for (Region region : this.store.values()) {
      if (region.contains(point)) {
        result.add(region);
      }
    }
    return result;
  }
}
