package tc.oc.pgm.filters.matcher.block;

import java.util.Collection;
import java.util.Collections;
import org.bukkit.event.Event;
import tc.oc.pgm.api.filter.Filter;
import tc.oc.pgm.api.filter.ReactorFactory;
import tc.oc.pgm.api.filter.query.LocationQuery;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.region.Region;
import tc.oc.pgm.filters.FilterMatchModule;
import tc.oc.pgm.filters.matcher.TypedFilter;
import tc.oc.pgm.regions.FiniteBlockRegion;
import tc.oc.pgm.regions.Union;
import tc.oc.pgm.regions.XMLRegionReference;
import tc.oc.pgm.util.event.PlayerCoarseMoveEvent;

public class BlocksFilter extends TypedFilter.Impl<LocationQuery>
    implements ReactorFactory<BlocksFilter.Reactor> {

  private final Region region;
  private final Filter filter;

  public BlocksFilter(Region region, Filter filter) {
    this.region = region;
    this.filter = filter;
  }

  @Override
  public boolean matches(LocationQuery query) {
    return query.reactor(this).region.contains(query);
  }

  @Override
  public Collection<Class<? extends Event>> getRelevantEvents() {
    return Collections.singleton(PlayerCoarseMoveEvent.class);
  }

  @Override
  public Class<? extends LocationQuery> queryType() {
    return LocationQuery.class;
  }

  @Override
  public Reactor createReactor(Match match, FilterMatchModule fmm) {
    return new Reactor(match, fmm, createBlockRegion(match, region));
  }

  private Region createBlockRegion(Match match, Region region) {
    if (region instanceof XMLRegionReference) region = ((XMLRegionReference) region).get();

    // Performance optimization: un-pack unions create finite regions of the inner regions, and
    // re-pack them as unions. Prevents large bounds with lots of empty space in between.
    if (region instanceof Union) {
      Region[] regions = ((Union) region).getRegions();
      Region[] result = new Region[regions.length];
      for (int i = 0; i < regions.length; i++) {
        result[i] = createBlockRegion(match, regions[i]);
      }
      return Union.of(result);
    }

    return FiniteBlockRegion.fromWorld(region, match.getWorld(), filter, match.getMap().getProto());
  }

  protected static final class Reactor extends ReactorFactory.Reactor {

    private final Region region;

    public Reactor(Match match, FilterMatchModule fmm, Region region) {
      super(match, fmm);
      this.region = region;
    }
  }
}
