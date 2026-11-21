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
import tc.oc.pgm.util.block.BlockVectors;

public class BuiltFilter extends TypedFilter.Impl<MatchQuery>
    implements ReactorFactory<BuiltFilter.Reactor> {

  public static final int MAX_VOLUME = 1024;

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
    return query.state(this).matches(query);
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
    private Structure struct;
    private Region.Static region;
    private BlockVector offset;

    public Reactor(Match match, FilterMatchModule fmm) {
      super(match, fmm);
      match.addListener(this, MatchScope.LOADED);
      ensureInit();
    }

    private boolean ensureInit() {
      if (struct != null) return true;
      struct = structure.get().getStructure(match);
      if (struct == null) return false;
      region = struct.getRegion().getStatic();
      offset = origin.clone().subtract(struct.getDefinition().getOrigin()).toBlockVector();
      return true;
    }

    public boolean matches(MatchQuery query) {
      if (!ensureInit()) return false;
      if (lastResult != null) return lastResult;

      var mismatch = struct.findMismatch(match.getWorld(), offset, lastMismatch);
      if (mismatch != null) lastMismatch.copy(mismatch);
      return lastResult = mismatch == null;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockTransform(BlockTransformEvent e) {
      if (!ensureInit()) return;
      var position = BlockVectors.position(e.getBlock());

      // Wasn't matching, and last mismatch position isn't here
      if (Boolean.FALSE.equals(lastResult) && !lastMismatch.equals(position)) return;
      if (!region.contains(position.clone().subtract(offset).toBlockVector())) return;

      lastResult = null;
      lastMismatch.copy(position);
      invalidate(BuiltFilter.this, match);
    }
  }
}
