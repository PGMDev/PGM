package tc.oc.pgm.filters.query;

import tc.oc.pgm.api.match.Match;

public interface IMatchQuery extends IQuery {
  Match getMatch();
}
