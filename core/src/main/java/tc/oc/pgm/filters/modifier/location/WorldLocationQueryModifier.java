package tc.oc.pgm.filters.modifier.location;

import org.bukkit.Location;
import org.bukkit.util.Vector;
import tc.oc.pgm.api.filter.Filter;
import tc.oc.pgm.api.filter.query.LocationQuery;

public class WorldLocationQueryModifier extends LocationQueryModifier {

  private final Vector coords;
  private final boolean[] relative;

  public WorldLocationQueryModifier(Filter filter, Vector coords, boolean[] relative) {
    super(filter);
    this.coords = coords;
    this.relative = relative;
  }

  @Override
  protected BlockQueryCustomLocation modifyQuery(LocationQuery query) {
    Location origin = query.getLocation();
    Location newLoc = origin.clone();
    newLoc.setX(relative[0] ? origin.getX() + coords.getX() : coords.getX());
    newLoc.setY(relative[1] ? origin.getY() + coords.getY() : coords.getY());
    newLoc.setZ(relative[2] ? origin.getZ() + coords.getZ() : coords.getZ());

    return new BlockQueryCustomLocation(query.getEvent(), newLoc);
  }
}
