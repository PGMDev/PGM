package tc.oc.pgm.filters.matcher.block;

import tc.oc.pgm.api.filter.query.BlockQuery;
import tc.oc.pgm.fallingblocks.FallingBlocksMatchModule;
import tc.oc.pgm.filters.matcher.TypedFilter;

/**
 * Tests the number of other gravity blocks that the queried block is supporting. This filter will
 * ABSTAIN unless the {@link tc.oc.pgm.fallingblocks.FallingBlocksModule} is loaded.
 *
 * <p>NOTE: this is potentially a very EXPENSIVE filter to apply, so XML authors should take care to
 * avoid evaluating it whenever possible, by placing other filters above it. They should be
 * particularly careful not to apply it to any events that modify large amounts of blocks all at
 * once, such as explosions.
 *
 * <p>The XML documentation should note all of this.
 */
public class StructuralLoadFilter extends TypedFilter.Impl<BlockQuery> {

  private final int threshold;

  public StructuralLoadFilter(int threshold) {
    this.threshold = threshold;
  }

  @Override
  public Class<? extends BlockQuery> queryType() {
    return BlockQuery.class;
  }

  @Override
  public boolean matches(BlockQuery query) {
    return query
        .moduleOptional(FallingBlocksMatchModule.class)
        .map(
            fbmm ->
                fbmm.countUnsupportedNeighbors(query.getBlock().getBlock(), threshold) >= threshold)
        .orElseGet(() -> 0 >= threshold);
  }
}
