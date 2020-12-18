package tc.oc.pgm.filters.modifier.location;

import javax.annotation.Nullable;
import org.bukkit.Location;
import org.bukkit.event.Event;
import tc.oc.pgm.api.filter.Filter;
import tc.oc.pgm.api.filter.query.LocationQuery;
import tc.oc.pgm.filters.modifier.QueryModifier;
import tc.oc.pgm.filters.query.BlockQuery;

public abstract class LocationQueryModifier extends QueryModifier<LocationQuery> {

  LocationQueryModifier(Filter filter) {
    super(filter);
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
}
