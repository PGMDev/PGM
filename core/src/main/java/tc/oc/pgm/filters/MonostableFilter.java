package tc.oc.pgm.filters;

import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import tc.oc.pgm.api.filter.Filter;
import tc.oc.pgm.api.filter.ReactorFactory;
import tc.oc.pgm.api.filter.query.MatchQuery;
import tc.oc.pgm.api.filter.query.Query;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.match.MatchScope;
import tc.oc.pgm.api.match.Tickable;
import tc.oc.pgm.api.time.Tick;
import tc.oc.pgm.filters.dynamic.FilterMatchModule;
import tc.oc.pgm.filters.dynamic.Filterable;
import tc.oc.pgm.filters.dynamic.Filterables;

public class MonostableFilter extends SingleFilterFunction
    implements ReactorFactory<MonostableFilter.Reactor> {

  private final Duration duration;
  final Class<? extends Filterable<?>> scope;

  public static Filter afterMatchStart(Duration duration) {
    return AllFilter.of(after(MatchPhaseFilter.RUNNING, duration), MatchPhaseFilter.RUNNING);
  }

  /**
   * Will rise after {@code duration} has passed after {@code filter} starts to rise.
   *
   * @param filter the filter to listen for changes to
   * @param duration the duration to delay this filters rise
   */
  public static Filter after(Filter filter, Duration duration) {
    return new InverseFilter(new MonostableFilter(filter, duration));
  }

  public MonostableFilter(Filter filter, Duration duration) {
    super(filter);
    this.duration = duration;
    this.scope = Filterables.scope(filter);
  }

  @Override
  public QueryResponse query(Query query) {
    boolean response = this.filter.response(query);
    if (!(query instanceof MatchQuery)) return QueryResponse.ABSTAIN; // TypedFilter<MatchQuery>
    final MatchQuery matchQuery = ((MatchQuery) query);
    final Filterable<?> filterable =
        Filterables.extractFilterable(matchQuery).getFilterableAncestor(this.scope);
    if (filterable == null)
      throw new IllegalArgumentException(
          "The scope of this filter does not match the query it received");

    final Reactor reactor =
        matchQuery.getMatch().needModule(FilterMatchModule.class).getReactor(this);

    return QueryResponse.fromBoolean(reactor.matches(filterable, response));
  }

  @Override
  public MonostableFilter.Reactor createReactor(Match match, FilterMatchModule fmm) {
    return new Reactor(match, fmm);
  }

  protected final class Reactor extends ReactorFactory.Reactor implements Tickable {

    // Filterables that currently pass the inner filter, mapped to the instants that they expire.
    // They are not actually removed until the inner filter goes false.
    final Map<Filterable<?>, Instant> endTimes = new HashMap<>();

    public Reactor(Match match, FilterMatchModule fmm) {
      super(match, fmm);
      match.addTickable(this, MatchScope.LOADED);
      fmm.onChange(Filterables.scope(filter), filter, this::matches);
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
