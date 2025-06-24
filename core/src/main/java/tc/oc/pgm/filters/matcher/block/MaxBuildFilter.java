package tc.oc.pgm.filters.matcher.block;

import tc.oc.pgm.api.filter.query.BlockQuery;
import tc.oc.pgm.api.filter.query.Query;
import tc.oc.pgm.filters.matcher.WeakTypedFilter;
import tc.oc.pgm.regions.RegionMatchModule;

/** Matches blocks that are above the max building height */
public class MaxBuildFilter implements WeakTypedFilter<BlockQuery> {

  public static final MaxBuildFilter INSTANCE = new MaxBuildFilter();

  private MaxBuildFilter() {}

  @Override
  public Class<? extends BlockQuery> queryType() {
    return BlockQuery.class;
  }

  @Override
  public boolean respondsTo(Class<? extends Query> queryType) {
    // We can't ever guarantee a response
    return false;
  }

  @Override
  public QueryResponse queryTyped(BlockQuery query) {
    return query
            .moduleOptional(RegionMatchModule.class)
            .map(RegionMatchModule::getMaxBuildHeight)
            .filter(maxBuild -> query.getBlock().getY() >= maxBuild)
            .isPresent()
        ? QueryResponse.DENY
        : QueryResponse.ABSTAIN;
  }

  @Override
  public String toString() {
    return "MaxBuildFilter{}";
  }
}
