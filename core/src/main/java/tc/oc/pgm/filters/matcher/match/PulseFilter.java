package tc.oc.pgm.filters.matcher.match;

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
import tc.oc.pgm.filters.operator.SingleFilterFunction;

public class PulseFilter extends SingleFilterFunction
    implements TypedFilter<MatchQuery>, ReactorFactory<PulseFilter.Reactor<?>> {

  private final long duration;
  private final long period;

  public PulseFilter(Filter filter, long duration, long period) {
    super(filter);
    this.duration = duration;
    this.period = period;
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

  protected class Reactor<F extends Filterable<?>> extends ReactorFactory.Reactor
      implements Tickable {

    protected final Class<F> scope;

    // Filterables that currently pass the inner filter, mapped to the tick they started matching.
    // They are not actually removed until the inner filter goes false.
    protected final Map<Filterable<?>, Long> startTimes = new HashMap<>();

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
        final long now = this.match.getTick().tick;

        Long start = startTimes.get(filterable);
        if (start == null) {
          // Cannot use computeIfAbsent, as we want invalidation after put
          startTimes.put(filterable, start = now);
          this.invalidate(filterable);
        }
        return ((now - start) % period) < duration;
      } else {
        if (startTimes.remove(filterable) != null) {
          this.invalidate(filterable);
        }
        return false;
      }
    }

    @Override
    public void tick(Match match, Tick tick) {
      final long now = tick.tick;

      startTimes.forEach(
          (filterable, start) -> {
            long ticks = (now - start) % period;
            if (ticks == 0 || ticks == duration) {
              this.invalidate(filterable);
            }
          });
    }
  }
}
