package tc.oc.pgm.filters.query;

import tc.oc.pgm.api.party.Party;

public interface IPartyQuery extends IMatchQuery {
  Party getParty();
}
