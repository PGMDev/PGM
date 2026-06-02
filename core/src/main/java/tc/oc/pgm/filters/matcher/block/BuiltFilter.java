package tc.oc.pgm.filters.matcher.block;

import org.bukkit.util.BlockVector;
import tc.oc.pgm.api.feature.FeatureReference;
import tc.oc.pgm.api.filter.query.MatchQuery;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.filters.matcher.TypedFilter;
import tc.oc.pgm.structure.StructureDefinition;

public class BuiltFilter extends TypedFilter.Impl<MatchQuery> {
  private final FeatureReference<StructureDefinition> structure;
  private final BlockVector origin;

  public BuiltFilter(FeatureReference<StructureDefinition> structure, BlockVector origin) {
    this.structure = structure;
    this.origin = origin;
  }

  @Override
  public boolean matches(MatchQuery query) {
    Match match = query.getMatch();
    var struct = structure.get().getStructure(match);

    return struct.matchesWorld(match.getWorld(), origin);
  }

  @Override
  public Class<? extends MatchQuery> queryType() {
    return MatchQuery.class;
  }

  @Override
  public String toString() {
    return "BuiltFilter{structure=" + structure.get().getId() + ", origin=" + origin + "}";
  }
}
