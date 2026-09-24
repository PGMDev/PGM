package tc.oc.pgm.filters.matcher.block;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.util.BlockVector;
import tc.oc.pgm.api.event.BlockTransformEvent;
import tc.oc.pgm.api.feature.FeatureReference;
import tc.oc.pgm.api.filter.ReactorFactory;
import tc.oc.pgm.api.filter.query.MatchQuery;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.match.MatchScope;
import tc.oc.pgm.api.region.Region;
import tc.oc.pgm.filters.FilterMatchModule;
import tc.oc.pgm.filters.matcher.TypedFilter;
import tc.oc.pgm.structure.Structure;
import tc.oc.pgm.structure.StructureDefinition;

public class BuiltFilter extends TypedFilter.Impl<MatchQuery>
    implements ReactorFactory<BuiltFilter.Reactor> {

  private final FeatureReference<StructureDefinition> structure;
  private final BlockVector origin;

  public BuiltFilter(FeatureReference<StructureDefinition> structure, BlockVector origin) {
    this.structure = structure;
    this.origin = origin;
  }

  @Override
  public boolean isDynamic() {
    return true;
  }

  @Override
  public boolean matches(MatchQuery query) {
    return ((Reactor) query.state(this)).matches(query);
  }

  @Override
  public Class<? extends MatchQuery> queryType() {
    return MatchQuery.class;
  }

  @Override
  public Reactor createReactor(Match match, FilterMatchModule fmm) {
    return new Reactor(match, fmm);
  }

  @Override
  public String toString() {
    return "BuiltFilter{structure=" + structure.get().getId() + ", origin=" + origin + "}";
  }

  public class Reactor extends ReactorFactory.Reactor implements Listener {
    private Boolean lastResult = null;
    private final BlockVector lastMismatch = origin.clone();
    private final BlockVector scratch = new BlockVector();
    private final Structure struct;
    private Region.Static region;
    private BlockVector offset;

    public Reactor(Match match, FilterMatchModule fmm) {
      super(match, fmm);
      match.addListener(this, MatchScope.LOADED);

      struct = structure.get().getStructure(match);
      if (struct != null) {
        region = struct.getRegion().getStatic();
        offset = origin.clone().subtract(struct.getDefinition().getOrigin()).toBlockVector();
      }
    }

    public boolean matches(MatchQuery query) {
      if (struct == null) return false;
      if (lastResult != null) return lastResult;

      boolean hasMismatch = struct.findMismatch(match.getWorld(), offset, lastMismatch);
      return lastResult = !hasMismatch;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockTransform(BlockTransformEvent e) {
      if (lastResult == null || struct == null) return;

      var block = e.getBlock();
      int x = block.getX(), y = block.getY(), z = block.getZ();

      boolean isMismatch = lastMismatch.getBlockX() == x
          && lastMismatch.getBlockY() == y
          && lastMismatch.getBlockZ() == z;

      scratch.setX(x - offset.getBlockX());
      scratch.setY(y - offset.getBlockY());
      scratch.setZ(z - offset.getBlockZ());

      boolean inRegion = lastResult && region.contains(scratch);

      if (isMismatch || inRegion) {
        lastResult = null;
        lastMismatch.setX(x);
        lastMismatch.setY(y);
        lastMismatch.setZ(z);
        invalidate(BuiltFilter.this, match);
      }
    }
  }
}
