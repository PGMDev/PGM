package tc.oc.pgm.filters.query;

import org.bukkit.Location;

public interface ILocationQuery extends IMatchQuery {
  Location getLocation();
}
