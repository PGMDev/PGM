package tc.oc.pgm.api.filter.query;

import tc.oc.pgm.api.party.Party;

public interface PartyQuery extends MatchQuery {
  Party getParty();
}
