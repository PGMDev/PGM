package tc.oc.pgm.filters;

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableList;
import java.util.List;
import tc.oc.pgm.filters.query.IQuery;

public abstract class MultiFilterFunction implements FilterDefinition {
  protected final List<Filter> filters;
  protected Class<? extends IQuery> upperBound;

  @Override
  public Class<? extends IQuery> getQueryType() {
    if (upperBound == null) {
      // Find the common ancestor of all child query types, starting with
      // an upper bound that will not be assignable from anything
      abstract class NullQuery implements IQuery {};
      this.upperBound = NullQuery.class;

      for (Filter child : this.filters) {
        if (child.getQueryType().isAssignableFrom(upperBound)) {
          upperBound = child.getQueryType();
        }
      }
    }

    return upperBound;
  }

  public MultiFilterFunction(Iterable<? extends Filter> filters) {
    this.filters = ImmutableList.copyOf(filters);
  }

  @Override
  public String toString() {
    return getClass().getSimpleName() + "{filters=" + Joiner.on(',').join(this.filters) + "}";
  }
}
