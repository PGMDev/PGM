package tc.oc.pgm.api.party.event;

import tc.oc.pgm.api.match.event.MatchEvent;
import tc.oc.pgm.api.party.Party;

/** Represents a {@link MatchEvent} tied to a {@link Party}. */
public abstract class PartyEvent extends MatchEvent {

  private final Party party;

  protected PartyEvent(Party party) {
    super(party.getMatch());
    this.party = party;
  }

  /**
   * Get the primary {@link Party} involved in the {@link PartyEvent}.
   *
   * @return The {@link Party}.
   */
  public final Party getParty() {
    return party;
  }
}
