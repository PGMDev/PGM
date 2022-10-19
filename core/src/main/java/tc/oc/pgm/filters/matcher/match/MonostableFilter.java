package tc.oc.pgm.filters.matcher.match;

import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import tc.oc.pgm.api.filter.Filter;
import tc.oc.pgm.api.filter.Filterables;
import tc.oc.pgm.api.filter.ReactorFactory;
import tc.oc.pgm.api.filter.query.MatchQuery;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.match.MatchScope;
import tc.oc.pgm.api.match.Tickable;
import tc.oc.pgm.api.time.Tick;
import tc.oc.pgm.filters.FilterMatchModule;
import tc.oc.pgm.filters.Filterable;
import tc.oc.pgm.filters.matcher.TypedFilter;
import tc.oc.pgm.filters.operator.AllFilter;
import tc.oc.pgm.filters.operator.InverseFilter;
import tc.oc.pgm.filters.operator.SingleFilterFunction;

public class MonostableFilter extends SingleFilterFunction
    implements TypedFilter<MatchQuery>, ReactorFactory<MonostableFilter.Reactor<?>> {

  private final Duration duration;

  public static Filter afterMatchStart(Duration duration) {
    return after(MatchPhaseFilter.RUNNING, duration);
  }

  /**
   * Will rise after {@code duration} has passed after {@code filter} starts to rise.
   *
   * @param filter the filter to listen for changes to
   * @param duration the duration to delay this filters rise
   */
  public static Filter after(Filter filter, Duration duration) {
    return AllFilter.of(filter, new InverseFilter(new MonostableFilter(filter, duration)));
  }

  public MonostableFilter(Filter filter, Duration duration) {
    super(filter);
    this.duration = duration;
  }

  @Override
  public Class<MatchQuery> queryType() {
    return MatchQuery.class;
  }

  @Override
  public boolean matches(MatchQuery query) {
    return query.reactor(this).matches(query);
  }

  @Override
  public Reactor<?> createReactor(Match match, FilterMatchModule fmm) {
    return new Reactor<>(match, fmm, Filterables.scope(filter));
  }

  protected final class Reactor<F extends Filterable<?>> extends ReactorFactory.Reactor
      implements Tickable {

    private final Class<F> scope;

    // Filterables that currently pass the inner filter, mapped to the instants that they expire.
    // They are not actually removed until the inner filter goes false.
    final Map<Filterable<?>, Instant> endTimes = new HashMap<>();

    public Reactor(Match match, FilterMatchModule fmm, Class<F> scope) {
      super(match, fmm);
      this.scope = scope;
      match.addTickable(this, MatchScope.LOADED);
      fmm.onChange(scope, filter, this::matches);
    }

    boolean matches(MatchQuery query) {
      final Filterable<?> filterable = query.filterable(this.scope);
      if (filterable == null) return false;

      return matches(filterable, filter.response(query));
    }

    boolean matches(Filterable<?> filterable, boolean response) {
      if (response) { // If inner filter still matches, check if the time has expired
        final Instant now = this.match.getTick().instant;
        final Instant end =
            endTimes.computeIfAbsent(
                filterable,
                f -> {
                  this.invalidate(filterable);
                  return now.plus(duration);
                });
        return now.isBefore(end);
      } else {
        if (endTimes.remove(filterable) != null) {
          this.invalidate(filterable);
        }
        return false;
      }
    }

    @Override
    public void tick(Match match, Tick tick) {
      final Instant now = tick.instant;

      endTimes.forEach(
          (filterable, end) -> {
            if (now.isAfter(end)) {
              this.invalidate(filterable);
            }
          });
    }
  }
}
