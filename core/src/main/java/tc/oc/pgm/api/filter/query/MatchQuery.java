package tc.oc.pgm.api.filter.query;

import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.filters.dynamic.Filterable;

public interface MatchQuery extends Query {
  Match getMatch();

  /** Extract the most specific {@link Filterable} possible from this query */
  default Filterable<?> extractFilterable() {
    if (this instanceof PlayerQuery) return this.getMatch().getPlayer(((PlayerQuery) this).getId());
    if (this instanceof PartyQuery) return ((PartyQuery) this).getParty();
    return this.getMatch();
  }
}
