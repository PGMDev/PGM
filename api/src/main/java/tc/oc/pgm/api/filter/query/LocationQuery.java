package tc.oc.pgm.api.filter.query;

import org.bukkit.Location;

public interface LocationQuery extends MatchQuery {
  Location getLocation();
}
