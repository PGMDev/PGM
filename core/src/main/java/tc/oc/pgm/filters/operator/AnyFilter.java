package tc.oc.pgm.filters.operator;

import java.util.ArrayList;
import java.util.Collection;
import tc.oc.pgm.api.filter.Filter;
import tc.oc.pgm.api.filter.query.Query;
import tc.oc.pgm.filters.matcher.block.MaterialFilter;
import tc.oc.pgm.util.material.MaterialMatcher;

public class AnyFilter extends MultiFilterFunction {

  public AnyFilter(Iterable<? extends Filter> filters) {
    super(filters);
  }

  @Override
  public QueryResponse query(Query query) {
    // returns true if any of the filters match
    QueryResponse response = QueryResponse.ABSTAIN;
    for (Filter filter : this.filters) {
      QueryResponse filterResponse = filter.query(query);
      if (filterResponse == QueryResponse.ALLOW) {
        return filterResponse;
      } else if (filterResponse == QueryResponse.DENY) {
        response = filterResponse;
      }
    }
    return response;
  }

  public static Filter of(Filter... filters) {
    return MultiFilterFunction.of(AnyFilter::mergeMaterials, filters);
  }

  public static Filter of(Collection<Filter> filters) {
    return MultiFilterFunction.of(AnyFilter::mergeMaterials, filters);
  }

  /**
   * Optimization when creating any -> materials type of filters. The material filters get unwrapped
   * and turned into just one material filter with a multi-material matcher.
   *
   * @param filters collection of the filters to wrap into an AnyFilter
   * @return an equivalent of AnyFilter, but with optimized materials
   */
  private static Filter mergeMaterials(Collection<Filter> filters) {
    var materials = MaterialMatcher.builder();
    var children = new ArrayList<Filter>(filters.size());
    for (Filter filter : filters) {
      if (filter instanceof MaterialFilter mf) materials.add(mf.getPattern());
      else children.add(filter);
    }
    if (!materials.isEmpty()) children.add(new MaterialFilter(materials.build()));
    return MultiFilterFunction.of(AnyFilter::new, children);
  }
}
