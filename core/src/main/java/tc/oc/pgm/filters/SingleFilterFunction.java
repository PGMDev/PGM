package tc.oc.pgm.filters;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.collect.ImmutableList;
import java.util.Collection;
import java.util.List;
import org.bukkit.event.Event;
import tc.oc.pgm.api.filter.Filter;
import tc.oc.pgm.api.filter.FilterDefinition;
import tc.oc.pgm.api.filter.query.Query;

/** A filter that transforms the result of a single child filter */
public abstract class SingleFilterFunction implements FilterDefinition, ParentFilter {

  protected final Filter filter;

  public SingleFilterFunction(Filter filter) {
    this.filter = checkNotNull(filter, "filter may not be null");
  }

  @Override
  public Collection<Class<? extends Event>> getRelevantEvents() {
    return this.filter.getRelevantEvents();
  }

  @Override
  public Class<? extends Query> getQueryType() {
    return filter.getQueryType();
  }

  @Override
  public List<Filter> getChildren() {
    return ImmutableList.of(this.filter);
  }

  @Override
  public String toString() {
    return getClass().getSimpleName() + "{filter=" + this.filter + "}";
  }
}
