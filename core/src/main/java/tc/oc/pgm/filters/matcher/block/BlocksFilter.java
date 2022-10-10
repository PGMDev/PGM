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
import tc.oc.pgm.util.event.PlayerCoarseMoveEvent;

public class BlocksFilter extends TypedFilter.Impl<LocationQuery>
    implements ReactorFactory<BlocksFilter.Reactor> {

  private final Region region;
  private final Filter child;

  public BlocksFilter(Region region, Filter child) {
    this.region = region;
    this.child = child;
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
    return new Reactor(
        match,
        fmm,
        FiniteBlockRegion.fromWorld(region, match.getWorld(), child, match.getMap().getProto()));
  }

  protected static final class Reactor extends ReactorFactory.Reactor {

    private final Region region;

    public Reactor(Match match, FilterMatchModule fmm, Region region) {
      super(match, fmm);
      this.region = region;
    }
  }
}
