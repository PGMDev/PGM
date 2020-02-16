package tc.oc.pgm.filters;

import tc.oc.pgm.fallingblocks.FallingBlocksMatchModule;
import tc.oc.pgm.filters.query.IBlockQuery;

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
public class StructuralLoadFilter extends TypedFilter<IBlockQuery> {

  private final int threshold;

  public StructuralLoadFilter(int threshold) {
    this.threshold = threshold;
  }

  @Override
  public Class<? extends IBlockQuery> getQueryType() {
    return IBlockQuery.class;
  }

  @Override
  protected QueryResponse queryTyped(IBlockQuery query) {
    FallingBlocksMatchModule fbmm = query.getMatch().getModule(FallingBlocksMatchModule.class);
    if (fbmm == null) return QueryResponse.ABSTAIN;

    int load = fbmm.countUnsupportedNeighbors(query.getBlock().getBlock(), this.threshold);
    return QueryResponse.fromBoolean(load >= this.threshold);
  }
}
