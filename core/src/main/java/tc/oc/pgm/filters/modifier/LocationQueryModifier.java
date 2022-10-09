package tc.oc.pgm.filters.modifier;

import org.bukkit.Location;
import org.bukkit.event.Event;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.Nullable;
import tc.oc.pgm.api.filter.Filter;
import tc.oc.pgm.api.filter.query.LocationQuery;
import tc.oc.pgm.filters.query.BlockQuery;

public abstract class LocationQueryModifier extends QueryModifier<LocationQuery, LocationQuery> {

  LocationQueryModifier(Filter filter) {
    super(filter, LocationQuery.class);
  }

  @Override
  public Class<? extends LocationQuery> queryType() {
    return LocationQuery.class;
  }

  /**
   * We transform all incoming {@link LocationQuery}s to this query before passing it onwards. This
   * action might discard some information (entities, damage causes...) but the use of this modifier
   * implies a need for checking the modified location for something else then the origin of the
   * query.
   */
  static final class BlockQueryCustomLocation extends BlockQuery {

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
    protected BlockQueryCustomLocation transformQuery(LocationQuery query) {
      Location origin = query.getLocation();
      Location newLoc = origin.clone();
      newLoc.setX(relative[0] ? origin.getX() + coords.getX() : coords.getX());
      newLoc.setY(relative[1] ? origin.getY() + coords.getY() : coords.getY());
      newLoc.setZ(relative[2] ? origin.getZ() + coords.getZ() : coords.getZ());

      return new BlockQueryCustomLocation(query.getEvent(), newLoc);
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
    protected BlockQueryCustomLocation transformQuery(LocationQuery query) {
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

      return new BlockQueryCustomLocation(query.getEvent(), newLoc);
    }
  }
}
