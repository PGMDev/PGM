package tc.oc.pgm.filters.operator;

import org.jdom2.Element;
import tc.oc.pgm.api.feature.FeatureReference;
import tc.oc.pgm.api.filter.Filter;
import tc.oc.pgm.api.filter.query.Query;

/**
 * Wrapper around a child filter, exists only as a parsing optimization. Single-child "any", "all"
 * or "one" filters can often be replaced with their single child, but when the child is a
 * reference, they can instead be turned into a wrapped filter. <br>
 * For example, {@code <all id="outer"><region id="other"/></all>}. Because the child is a
 * reference, if we simplified parsing to returning "other", pgm wouldn't register it under the
 * "outer" name (you can't register a reference, otherwise it creates duplicates). For those cases,
 * we create a filter wrapper around "other", and return that instead.
 */
public class FilterWrapper extends SingleFilterFunction {

  private FilterWrapper(Filter filter) {
    super(filter);
  }

  @Override
  public QueryResponse query(Query query) {
    return filter.query(query);
  }

  public static Filter of(Element el, Filter filter) {
    if (el.getAttribute("id") == null || !(filter instanceof FeatureReference<?>)) return filter;
    return new FilterWrapper(filter);
  }
}
