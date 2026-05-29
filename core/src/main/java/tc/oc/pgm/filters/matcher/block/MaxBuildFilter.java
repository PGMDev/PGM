package tc.oc.pgm.filters.matcher.block;

import tc.oc.pgm.api.filter.Filter;
import tc.oc.pgm.api.filter.query.BlockQuery;
import tc.oc.pgm.api.filter.query.Query;
import tc.oc.pgm.filters.matcher.StaticFilter;
import tc.oc.pgm.filters.matcher.WeakTypedFilter;
import tc.oc.pgm.filters.operator.SingleFilterFunction;
import tc.oc.pgm.regions.RegionMatchModule;

/** Matches blocks that are above the max building height */
public class MaxBuildFilter extends SingleFilterFunction implements WeakTypedFilter<BlockQuery> {
  private static final MaxBuildFilter DENY = new MaxBuildFilter(StaticFilter.DENY);

  public static MaxBuildFilter of(Filter filter) {
    if (filter == StaticFilter.DENY) return DENY;
    return new MaxBuildFilter(filter);
  }

  private MaxBuildFilter(final Filter filter) {
    super(filter);
  }

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
        ? filter.query(query)
        : QueryResponse.ABSTAIN;
  }

  @Override
  public String toString() {
    return "MaxBuildFilter{filter=" + filter + "}";
  }
}
