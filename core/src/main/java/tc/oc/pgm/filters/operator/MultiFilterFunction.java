package tc.oc.pgm.filters.operator;

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.bukkit.event.Event;
import tc.oc.pgm.api.filter.Filter;
import tc.oc.pgm.api.filter.FilterDefinition;
import tc.oc.pgm.api.filter.query.Query;
import tc.oc.pgm.filters.matcher.StaticFilter;

public abstract class MultiFilterFunction implements FilterDefinition {
  protected final List<Filter> filters;

  public MultiFilterFunction(Iterable<? extends Filter> filters) {
    this.filters = ImmutableList.copyOf(filters);
  }

  @Override
  public boolean respondsTo(Class<? extends Query> queryType) {
    return !filters.isEmpty() && filters.stream().allMatch(f -> f.respondsTo(queryType));
  }

  @Override
  public boolean isDynamic() {
    return this.filters.stream().allMatch(Filter::isDynamic);
  }

  @Override
  public Collection<Class<? extends Event>> getRelevantEvents() {
    return this.filters.stream()
        .flatMap(f -> f.getRelevantEvents().stream())
        .collect(Collectors.collectingAndThen(Collectors.toList(), ImmutableList::copyOf));
  }

  @Override
  public Stream<? extends Filter> dependencies() {
    return filters.stream();
  }

  @Override
  public String toString() {
    return getClass().getSimpleName() + "{filters=" + Joiner.on(',').join(this.filters) + "}";
  }

  public static Filter of(Function<Collection<Filter>, Filter> builder, Filter... filters) {
    return switch (filters.length) {
      case 0 -> StaticFilter.ABSTAIN;
      case 1 -> filters[0];
      default -> builder.apply(Arrays.asList(filters));
    };
  }

  public static Filter of(
      Function<Collection<Filter>, Filter> builder, Collection<Filter> filters) {
    return switch (filters.size()) {
      case 0 -> StaticFilter.ABSTAIN;
      case 1 -> filters.iterator().next();
      default -> builder.apply(filters);
    };
  }
}
