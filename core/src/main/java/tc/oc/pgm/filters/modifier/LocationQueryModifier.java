package tc.oc.pgm.filters.modifier;

import org.bukkit.Location;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockRedstoneEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.util.Vector;
import tc.oc.pgm.api.event.BlockTransformEvent;
import tc.oc.pgm.api.filter.Filter;
import tc.oc.pgm.api.filter.ReactorFactory;
import tc.oc.pgm.api.filter.query.BlockQuery;
import tc.oc.pgm.api.filter.query.LocationQuery;
import tc.oc.pgm.api.filter.query.MatchQuery;
import tc.oc.pgm.api.filter.query.MaterialQuery;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.match.MatchScope;
import tc.oc.pgm.filters.FilterMatchModule;
import tc.oc.pgm.util.math.OffsetVector;

public class LocationQueryModifier extends QueryModifier<LocationQuery, BlockQuery> {

  private final OffsetVector offset;

  private LocationQueryModifier(Filter filter, OffsetVector offset) {
    super(filter, LocationQuery.class, BlockQuery.class);
    this.offset = offset;
  }

  @Override
  protected BlockQuery transformQuery(LocationQuery query) {
    return new tc.oc.pgm.filters.query.BlockQuery(
        query.getEvent(), offset.applyOffset(query.getLocation()));
  }

  public static Filter of(Filter child, OffsetVector offset) {
    if (offset.isAbsolute()) return new Absolute(child, offset.getVector());
    return new LocationQueryModifier(child, offset);
  }

  /**
   * Specialization when the requested location is an absolute position. It can work without an
   * initial location query, and becomes dynamic as long as the inner filter can respond to material
   * queries.
   */
  private static class Absolute extends QueryModifier<MatchQuery, BlockQuery>
      implements ReactorFactory<Absolute.Reactor> {

    private final Vector location;

    private Absolute(Filter filter, Vector location) {
      super(filter, MatchQuery.class, BlockQuery.class);
      this.location = location;
    }

    @Override
    protected BlockQuery transformQuery(MatchQuery query) {
      return new tc.oc.pgm.filters.query.BlockQuery(
          query.getEvent(), location.toLocation(query.getMatch().getWorld()));
    }

    @Override
    public boolean isDynamic() {
      return filter.respondsTo(MaterialQuery.class);
    }

    @Override
    public Reactor createReactor(Match match, FilterMatchModule fmm) {
      return new Reactor(match, fmm);
    }

    private class Reactor extends ReactorFactory.Reactor implements Listener {
      public Reactor(Match match, FilterMatchModule fmm) {
        super(match, fmm);
        match.addListener(this, MatchScope.RUNNING);
      }

      private void invalidate(Location modified) {
        if (modified.getBlockX() == location.getBlockX()
            && modified.getBlockY() == location.getBlockY()
            && modified.getBlockZ() == location.getBlockZ()) {
          invalidate(Absolute.this, match);
        }
      }

      @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
      public void onBlockTransform(BlockTransformEvent e) {
        invalidate(e.getBlock().getLocation());
      }

      @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
      public void onBlockInteract(PlayerInteractEvent e) {
        // Clicking on doors or trapdoors
        if (e.getAction() == Action.RIGHT_CLICK_BLOCK && e.hasBlock())
          invalidate(e.getClickedBlock().getLocation());
      }

      @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
      public void onRedstoneChange(BlockRedstoneEvent e) {
        // Buttons, pressure plates, or other redstone circuits changing
        invalidate(e.getBlock().getLocation());
      }
    }
  }
}
