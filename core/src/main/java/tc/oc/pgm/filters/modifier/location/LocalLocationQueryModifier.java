package tc.oc.pgm.filters.modifier.location;

import org.bukkit.Location;
import org.bukkit.util.Vector;
import tc.oc.pgm.api.filter.Filter;
import tc.oc.pgm.api.filter.query.LocationQuery;

public class LocalLocationQueryModifier extends LocationQueryModifier {
  private final Vector coords;

  public LocalLocationQueryModifier(Filter child, Vector coords) {
    super(child);
    this.coords = coords;
  }

  @Override
  protected BlockQueryCustomLocation modifyQuery(LocationQuery query) {
    Location origin = query.getLocation();
    double x = coords.getX();
    double y = coords.getY();
    double z = coords.getZ();
    Vector dirZ = origin.getDirection().normalize();
    Location newLoc = origin.clone().add(dirZ.multiply(z));

    float yaw = newLoc.getYaw() - 90;
    Vector dirX = new Vector(-Math.sin(Math.toRadians(yaw)), 0, Math.cos(Math.toRadians(yaw)));
    newLoc = newLoc.add(dirX.multiply(x));

    float pitch = newLoc.getPitch() - 90;
    Vector dirY = new Vector(0, -Math.sin(Math.toRadians(pitch)), Math.cos(Math.toRadians(pitch)));
    newLoc = newLoc.add(dirY.multiply(y));

    return new BlockQueryCustomLocation(query.getEvent(), newLoc);
  }
}
