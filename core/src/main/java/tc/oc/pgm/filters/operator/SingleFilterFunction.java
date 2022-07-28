package tc.oc.pgm.filters.operator;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Collection;
import java.util.stream.Stream;
import org.bukkit.event.Event;
import tc.oc.pgm.api.filter.Filter;
import tc.oc.pgm.api.filter.FilterDefinition;
import tc.oc.pgm.api.filter.FilterTypeException;
import tc.oc.pgm.api.filter.query.Query;

/** A filter that transforms the result of a single child filter */
public abstract class SingleFilterFunction implements FilterDefinition {

  protected final Filter filter;

  public SingleFilterFunction(Filter filter) {
    this.filter = checkNotNull(filter, "filter may not be null");
  }

  @Override
  public boolean respondsTo(Class<? extends Query> queryType) {
    return filter.respondsTo(queryType);
  }

  @Override
  public void assertRespondsTo(Class<? extends Query> queryType) throws FilterTypeException {
    filter.assertRespondsTo(queryType);
  }

  @Override
  public boolean isDynamic() {
    return filter.isDynamic();
  }

  @Override
  public Collection<Class<? extends Event>> getRelevantEvents() {
    return this.filter.getRelevantEvents();
  }

  @Override
  public Stream<? extends Filter> dependencies() {
    return Stream.of(this.filter);
  }

  @Override
  public String toString() {
    return getClass().getSimpleName() + "{filter=" + this.filter + "}";
  }
}
