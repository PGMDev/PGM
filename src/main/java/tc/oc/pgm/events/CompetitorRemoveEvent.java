package tc.oc.pgm.events;

import tc.oc.pgm.match.Competitor;

public class CompetitorRemoveEvent extends PartyRemoveEvent {

  public CompetitorRemoveEvent(Competitor competitor) {
    super(competitor);
  }

  public Competitor getCompetitor() {
    return (Competitor) getParty();
  }
}
