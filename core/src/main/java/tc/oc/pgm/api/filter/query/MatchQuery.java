package tc.oc.pgm.api.filter.query;

import tc.oc.pgm.api.match.Match;

public interface MatchQuery extends Query {
  Match getMatch();
}
