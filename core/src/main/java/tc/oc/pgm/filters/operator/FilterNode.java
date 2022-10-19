package tc.oc.pgm.filters.operator;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import tc.oc.pgm.api.filter.Filter;
import tc.oc.pgm.api.filter.FilterDefinition;
import tc.oc.pgm.api.filter.query.Query;

@Deprecated
public class FilterNode implements FilterDefinition {
  protected final List<Filter> parents;
  protected final List<Filter> allowedMatchers;
  protected final List<Filter> deniedMatchers;

  public FilterNode(
      List<Filter> parents, List<Filter> allowedMatchers, List<Filter> deniedMatchers) {
    this.parents = parents;
    this.allowedMatchers = allowedMatchers;
    this.deniedMatchers = deniedMatchers;
  }

  @Override
  public boolean respondsTo(Class<? extends Query> queryType) {
    // Legacy filter, does not need to support dynamic stuff at all.
    return false;
  }

  protected boolean matches(Collection<Filter> matchers, Query query) {
    for (Filter matcher : matchers) {
      if (matcher.query(query) == QueryResponse.ALLOW) {
        return true;
      }
    }
    return false;
  }

  public QueryResponse queryParents(Query query) {
    QueryResponse response = QueryResponse.ABSTAIN;
    for (Filter parent : this.parents) {
      QueryResponse parentResponse = parent.query(query);
      if (parentResponse != QueryResponse.ABSTAIN) {
        response = parentResponse;
      }
    }
    return response;
  }

  @Override
  public QueryResponse query(Query query) {
    if (matches(deniedMatchers, query)) {
      return QueryResponse.DENY;
    } else if (matches(allowedMatchers, query)) {
      return QueryResponse.ALLOW;
    } else {
      return queryParents(query);
    }
  }

  public static FilterNode allow(Filter child) {
    return new FilterNode(
        Collections.<Filter>emptyList(),
        Collections.singletonList(child),
        Collections.<Filter>emptyList());
  }

  public static FilterNode deny(Filter child) {
    return new FilterNode(
        Collections.<Filter>emptyList(),
        Collections.<Filter>emptyList(),
        Collections.singletonList(child));
  }
}
