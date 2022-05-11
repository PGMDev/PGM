package tc.oc.pgm.api.filter.query;

import org.bukkit.Location;
import tc.oc.pgm.util.block.BlockVectors;

public interface LocationQuery extends MatchQuery {
  Location getLocation();

  default Location getBlockCenter() {
    return BlockVectors.center(getLocation());
  }
}
