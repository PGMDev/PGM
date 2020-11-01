package tc.oc.pgm.filters.modifier.location;

import org.bukkit.Location;
import tc.oc.pgm.api.filter.Filter;
import tc.oc.pgm.api.filter.query.LocationQuery;

public class WorldLocationQueryModifier extends LocationQueryModifier {

  private final double[] coords;
  private final boolean[] relative;

  public WorldLocationQueryModifier(Filter filter, double[] coords, boolean[] relative) {
    super(filter);
    this.coords = coords;
    this.relative = relative;
  }

  @Override
  protected BlockQueryCustomLocation modifyQuery(LocationQuery query) {
    Location origin = query.getLocation();
    Location newLoc = origin.clone();
    newLoc.setX(relative[0] ? origin.getX() + coords[0] : coords[0]);
    newLoc.setY(relative[1] ? origin.getY() + coords[1] : coords[1]);
    newLoc.setZ(relative[2] ? origin.getZ() + coords[2] : coords[2]);

    return new BlockQueryCustomLocation(query.getEvent(), newLoc);
  }
}
