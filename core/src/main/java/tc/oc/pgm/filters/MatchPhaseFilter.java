package tc.oc.pgm.filters;

import tc.oc.pgm.api.filter.Filter;
import tc.oc.pgm.api.filter.query.MatchQuery;
import tc.oc.pgm.api.match.MatchPhase;

public class MatchPhaseFilter extends TypedFilter<MatchQuery> {

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
  public Class<? extends MatchQuery> getQueryType() {
    return MatchQuery.class;
  }

  @Override
  protected QueryResponse queryTyped(MatchQuery query) {
    MatchPhase current = query.getMatch().getPhase();
    return QueryResponse.fromBoolean(matchPhase == current);
  }
}
