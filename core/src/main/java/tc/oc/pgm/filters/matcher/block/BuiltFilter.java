package tc.oc.pgm.filters.matcher.block;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import org.bukkit.Location;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.util.BlockVector;
import tc.oc.pgm.api.PGM;
import tc.oc.pgm.api.event.BlockTransformEvent;
import tc.oc.pgm.api.feature.FeatureReference;
import tc.oc.pgm.api.filter.ReactorFactory;
import tc.oc.pgm.api.filter.query.MatchQuery;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.match.MatchScope;
import tc.oc.pgm.filters.FilterMatchModule;
import tc.oc.pgm.filters.matcher.TypedFilter;
import tc.oc.pgm.structure.Structure;
import tc.oc.pgm.structure.StructureDefinition;

public class BuiltFilter extends TypedFilter.Impl<MatchQuery>
    implements ReactorFactory<BuiltFilter.Reactor> {

  private static final int MAX_BLOCKS = 1024;
  private static final String OVERSIZED_ERR =
      "Structure '%s' used in 'built' filter has %d blocks, exceeds limit of %d. Filter will always return false";

  private final FeatureReference<StructureDefinition> structure;
  private final BlockVector origin;

  private final Map<Match, Reactor> reactors = new ConcurrentHashMap<>();
  private Boolean oversized;

  public BuiltFilter(FeatureReference<StructureDefinition> structure, BlockVector origin) {
    this.structure = structure;
    this.origin = origin;
  }

  private boolean isOversized() {
    if (oversized != null) return oversized;

    int count = 0;
    var it = structure.get().getRegion().getStatic().getBlockVectorIterator();
    while (it.hasNext()) {
      it.next();
      count++;
    }

    oversized = count > MAX_BLOCKS;
    if (oversized) {
      PGM.get()
          .getGameLogger()
          .log(
              Level.SEVERE,
              String.format(OVERSIZED_ERR, structure.get().getId(), count, MAX_BLOCKS));
    }
    return oversized;
  }

  @Override
  public boolean isDynamic() {
    return !isOversized();
  }

  @Override
  public boolean matches(MatchQuery query) {
    if (isOversized()) return false;

    Match match = query.getMatch();
    return reactors.computeIfAbsent(match, m -> new Reactor(m, null)).evaluate();
  }

  @Override
  public Class<? extends MatchQuery> queryType() {
    return MatchQuery.class;
  }

  @Override
  public Reactor createReactor(Match match, FilterMatchModule fmm) {
    return reactors.computeIfAbsent(match, m -> new Reactor(m, fmm));
  }

  @Override
  public String toString() {
    return "BuiltFilter{structure=" + structure.get().getId() + ", origin=" + origin + "}";
  }

  public class Reactor extends ReactorFactory.Reactor implements Listener {
    private Boolean cachedResult;
    private BlockVector cachedMismatch;

    public Reactor(Match match, FilterMatchModule fmm) {
      super(match, fmm);
      match.addListener(this, MatchScope.RUNNING);
    }

    public boolean evaluate() {
      if (cachedResult != null) return cachedResult;

      Structure struct = structure.get().getStructure(match);
      cachedMismatch = struct.findMismatch(match.getWorld(), origin);
      cachedResult = cachedMismatch == null;
      return cachedResult;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockTransform(BlockTransformEvent e) {
      if (cachedResult == null) return;

      Location loc = e.getBlock().getLocation();
      boolean relevant;

      if (cachedMismatch != null) {
        relevant = loc.getBlockX() == cachedMismatch.getBlockX()
            && loc.getBlockY() == cachedMismatch.getBlockY()
            && loc.getBlockZ() == cachedMismatch.getBlockZ();
      } else {
        Structure struct = structure.get().getStructure(match);
        BlockVector offset =
            origin.clone().subtract(struct.getDefinition().getOrigin()).toBlockVector();
        Location relative =
            loc.clone().subtract(offset.getBlockX(), offset.getBlockY(), offset.getBlockZ());
        relevant = struct.getRegion().contains(relative);
      }

      if (relevant) {
        cachedResult = null;
        cachedMismatch = null;
        invalidate(BuiltFilter.this, match);
      }
    }
  }
}
