package tc.oc.pgm.events;

import tc.oc.pgm.match.Competitor;

public class CompetitorAddEvent extends PartyAddEvent {

  public CompetitorAddEvent(Competitor competitor) {
    super(competitor);
  }

  public Competitor getCompetitor() {
    return (Competitor) getParty();
  }
}
