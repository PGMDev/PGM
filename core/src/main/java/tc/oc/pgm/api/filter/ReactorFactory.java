package tc.oc.pgm.api.filter;

import org.bukkit.event.Event;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.match.event.MatchLoadEvent;
import tc.oc.pgm.filters.FilterMatchModule;
import tc.oc.pgm.filters.Filterable;

/**
 * Implemented by filters which can act dynamically but needs more than Bukkit {@link Event}s to
 * properly invalidate {@link Filterable}s. A filter can theoretically both depend on listening to
 * some events AND have a reactor, but this is rarely the case.
 */
public interface ReactorFactory<R extends ReactorFactory.Reactor> extends FilterDefinition {

  /**
   * Get an instance of this filter's reactor. This will only be called once per match and always
   * before querying the filter with any queries. This will always be called even when this filter
   * is not used dynamically.
   *
   * @param match the match this filter is active in
   */
  R createReactor(Match match, FilterMatchModule fmm);

  /**
   * A match scoped singleton responsible for match time invalidation of filterables that the {@link
   * ReactorFactory} that created this might have changed its opinion about. This is created at the
   * end of match load.
   *
   * @see FilterMatchModule#onMatchLoad(MatchLoadEvent)
   */
  abstract class Reactor {

    private final FilterMatchModule fmm;
    protected final Match match;

    public Reactor(Match match, FilterMatchModule fmm) {
      this.match = match;
      this.fmm = fmm;
    }

    /** Called at the end of each match for reactors that need cleanup */
    public void unload() {}

    protected void invalidate(Filterable<?> filterable) {
      this.fmm.invalidate(filterable);
    }
  }
}
