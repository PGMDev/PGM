package tc.oc.pgm.filters;

import java.util.function.Predicate;
import tc.oc.pgm.api.filter.query.MatchQuery;
import tc.oc.pgm.api.match.Match;

public class MatchStateFilter extends TypedFilter<MatchQuery> {

  private final Predicate<Match> matchPredicate;

  public MatchStateFilter(Predicate<Match> matchPredicate) {
    this.matchPredicate = matchPredicate;
  }

  @Override
  public Class<? extends MatchQuery> getQueryType() {
    return MatchQuery.class;
  }

  @Override
  protected QueryResponse queryTyped(MatchQuery query) {
    return QueryResponse.fromBoolean(matchPredicate.test(query.getMatch()));
  }
}
