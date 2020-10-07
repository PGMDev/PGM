package tc.oc.pgm.filters;

import javax.annotation.Nullable;
import org.bukkit.Location;
import org.bukkit.event.Event;
import org.bukkit.util.Vector;
import tc.oc.pgm.api.filter.Filter;
import tc.oc.pgm.api.filter.query.LocationQuery;
import tc.oc.pgm.filters.query.BlockQuery;

public class LocationQueryModifier extends QueryModifier<LocationQuery> {

  private final String[] coords;
  private final boolean local;
  Filter child;

  public LocationQueryModifier(Filter child, String[] coords, boolean local) {
    super(child);
    this.coords = coords;
    this.local = local;
    this.child = child;
  }

  @Override
  protected LocationQuery modifyQuery(LocationQuery query) {
    Location location = query.getLocation();

    Vector newVector;

    if (local) {
      newVector = parseLocalLocation(query.getLocation(), coords[0], coords[1], coords[2]);
    } else {
      newVector =
          new Vector(
              parseRelativeLocation(coords[0], location.getX()),
              parseRelativeLocation(coords[1], location.getY()),
              parseRelativeLocation(coords[2], location.getZ()));
    }

    return new BlockQueryCustomLocation(
        query.getEvent(), newVector.toLocation(query.getMatch().getWorld()));
  }

  private double parseRelativeLocation(String coordinate, double originalLocationCoordinate) {
    if (coordinate.startsWith("~")) {
      return originalLocationCoordinate + Double.parseDouble(coordinate.substring(1));
    }
    return Double.parseDouble(coordinate);
  }

  private Vector parseLocalLocation(Location origin, String x, String y, String z) {

    double x1 = Double.parseDouble(x.substring(1));
    double y1 = Double.parseDouble(y.substring(1));
    double z1 = Double.parseDouble(z.substring(1));
    Vector dirZ = origin.getDirection().normalize();
    Location newLoc = origin.clone().add(dirZ.multiply(z1));

    float yaw = newLoc.getYaw() - 90;
    Vector dirX = new Vector(-Math.sin(Math.toRadians(yaw)), 0, Math.cos(Math.toRadians(yaw)));
    newLoc = newLoc.add(dirX.multiply(x1));

    float pitch = newLoc.getPitch() - 90;
    Vector dirY = new Vector(0, -Math.sin(Math.toRadians(pitch)), Math.cos(Math.toRadians(pitch)));
    newLoc = newLoc.add(dirY.multiply(y1));

    return new Vector(newLoc.getX(), newLoc.getY(), newLoc.getZ());
  }

  @Override
  public Class<? extends LocationQuery> getQueryType() {
    return LocationQuery.class;
  }

  /**
   * We transform all incoming {@link LocationQuery}s to this query before passing it onwards. This
   * action might discard some information (entities, damage causes...) but the use of this modifier
   * implies a need for checking the modified location for something else then the origin of the
   * query.
   */
  private static final class BlockQueryCustomLocation extends BlockQuery {

    private final Location modifiedLocation;

    public BlockQueryCustomLocation(@Nullable Event event, Location modifiedLocation) {
      super(event, modifiedLocation.getBlock());
      this.modifiedLocation = modifiedLocation;
    }

    /** This is the precise location, getBlock()#getLocation() and similar will NOT be precise */
    @Override
    public Location getLocation() {
      return modifiedLocation;
    }
  }
}
