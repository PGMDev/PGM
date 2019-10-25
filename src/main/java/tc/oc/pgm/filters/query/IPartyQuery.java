package tc.oc.pgm.filters.query;

import tc.oc.pgm.match.Party;

public interface IPartyQuery extends IMatchQuery {
  Party getParty();
}
