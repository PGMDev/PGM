package tc.oc.pgm.filters;

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableList;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import org.bukkit.event.Event;
import tc.oc.pgm.api.filter.Filter;
import tc.oc.pgm.api.filter.FilterDefinition;
import tc.oc.pgm.api.filter.query.Query;

public abstract class MultiFilterFunction implements FilterDefinition {
  protected final List<Filter> filters;
  private final Collection<Class<? extends Event>> dynamicEvents;
  protected Class<? extends Query> upperBound;

  @Override
  public Class<? extends Query> getQueryType() {
    if (upperBound == null) {
      // Find the common ancestor of all child query types, starting with
      // an upper bound that will not be assignable from anything
      abstract class NullQuery implements Query {};
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

    final List<Class<? extends Event>> allEvents = new ArrayList<>();
    this.filters.forEach(filter -> allEvents.addAll(filter.getRelevantEvents()));
    this.dynamicEvents = ImmutableList.copyOf(allEvents);
  }

  @Override
  public Collection<Class<? extends Event>> getRelevantEvents() {
    return this.dynamicEvents;
  }

  @Override
  public String toString() {
    return getClass().getSimpleName() + "{filters=" + Joiner.on(',').join(this.filters) + "}";
  }
}
