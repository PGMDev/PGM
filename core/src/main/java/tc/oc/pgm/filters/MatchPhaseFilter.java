package tc.oc.pgm.filters;

import java.util.Set;
import tc.oc.pgm.api.filter.query.MatchQuery;
import tc.oc.pgm.api.match.MatchPhase;

public class MatchPhaseFilter extends TypedFilter<MatchQuery> {

  private final Set<MatchPhase> matchPhase;

  public MatchPhaseFilter(Set<MatchPhase> matchPhase) {
    this.matchPhase = matchPhase;
  }

  @Override
  public Class<? extends MatchQuery> getQueryType() {
    return MatchQuery.class;
  }

  @Override
  protected QueryResponse queryTyped(MatchQuery query) {
    MatchPhase current = query.getMatch().getPhase();
    return QueryResponse.fromBoolean(matchPhase.stream().anyMatch(mp -> mp == current));
  }
}
