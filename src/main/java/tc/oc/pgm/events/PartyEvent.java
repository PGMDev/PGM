package tc.oc.pgm.events;

import tc.oc.pgm.match.Party;

public abstract class PartyEvent extends MatchEvent {

  protected final Party party;

  protected PartyEvent(Party party) {
    super(party.getMatch());
    this.party = party;
  }

  public Party getParty() {
    return party;
  }
}
