package tc.oc.pgm.filters.matcher.match;

import java.util.Collection;
import java.util.Collections;
import org.bukkit.event.Event;
import tc.oc.pgm.api.filter.Filter;
import tc.oc.pgm.api.filter.query.MatchQuery;
import tc.oc.pgm.api.match.MatchPhase;
import tc.oc.pgm.api.match.event.MatchPhaseChangeEvent;
import tc.oc.pgm.filters.matcher.TypedFilter;
import tc.oc.pgm.filters.operator.AnyFilter;

public class MatchPhaseFilter extends TypedFilter.Impl<MatchQuery> {

  public static final MatchPhaseFilter RUNNING = new MatchPhaseFilter(MatchPhase.RUNNING);
  public static final MatchPhaseFilter FINISHED = new MatchPhaseFilter(MatchPhase.FINISHED);
  public static final MatchPhaseFilter STARTING = new MatchPhaseFilter(MatchPhase.STARTING);
  public static final MatchPhaseFilter IDLE = new MatchPhaseFilter(MatchPhase.IDLE);
  public static final Filter STARTED =
      AnyFilter.of(
          new MatchPhaseFilter(MatchPhase.RUNNING), new MatchPhaseFilter(MatchPhase.FINISHED));

  private final MatchPhase matchPhase;

  public MatchPhaseFilter(MatchPhase matchPhase) {
    this.matchPhase = matchPhase;
  }

  @Override
  public Collection<Class<? extends Event>> getRelevantEvents() {
    return Collections.singleton(MatchPhaseChangeEvent.class);
  }

  @Override
  public Class<? extends MatchQuery> queryType() {
    return MatchQuery.class;
  }

  @Override
  public boolean matches(MatchQuery query) {
    return matchPhase == query.getMatch().getPhase();
  }
}
