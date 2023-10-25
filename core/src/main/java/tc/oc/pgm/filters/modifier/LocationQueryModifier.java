package tc.oc.pgm.filters.modifier;

import org.bukkit.Location;
import org.bukkit.util.Vector;
import tc.oc.pgm.api.filter.Filter;
import tc.oc.pgm.api.filter.query.BlockQuery;
import tc.oc.pgm.api.filter.query.LocationQuery;

public abstract class LocationQueryModifier extends QueryModifier<LocationQuery, BlockQuery> {

  LocationQueryModifier(Filter filter) {
    super(filter, BlockQuery.class);
  }

  @Override
  public Class<? extends LocationQuery> queryType() {
    return LocationQuery.class;
  }

  /** Uses world coordinates (x, y, z) in either absolute or relative form */
  public static class World extends LocationQueryModifier {

    private final Vector coords;
    private final boolean[] relative;

    public World(Filter filter, Vector coords, boolean[] relative) {
      super(filter);
      this.coords = coords;
      this.relative = relative;
    }

    @Override
    protected BlockQuery transformQuery(LocationQuery query) {
      Location origin = query.getLocation();
      Location newLoc = origin.clone();
      newLoc.setX(relative[0] ? origin.getX() + coords.getX() : coords.getX());
      newLoc.setY(relative[1] ? origin.getY() + coords.getY() : coords.getY());
      newLoc.setZ(relative[2] ? origin.getZ() + coords.getZ() : coords.getZ());

      return new tc.oc.pgm.filters.query.BlockQuery(query.getEvent(), newLoc);
    }
  }

  /**
   * Uses local coordinates (left, up, front), which are always relative. These coordinates are also
   * known as caret notation or ^ΔSway ^ΔHeave ^ΔSurge
   */
  public static class Local extends LocationQueryModifier {
    private final Vector coords;

    public Local(Filter child, Vector coords) {
      super(child);
      this.coords = coords;
    }

    @Override
    protected BlockQuery transformQuery(LocationQuery query) {
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
      Vector dirY =
          new Vector(0, -Math.sin(Math.toRadians(pitch)), Math.cos(Math.toRadians(pitch)));
      newLoc = newLoc.add(dirY.multiply(y));

      return new tc.oc.pgm.filters.query.BlockQuery(query.getEvent(), newLoc);
    }
  }
}
