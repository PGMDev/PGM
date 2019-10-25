package tc.oc.pgm.filters.query;

import tc.oc.pgm.match.Match;

public interface IMatchQuery extends IQuery {
  Match getMatch();
}
