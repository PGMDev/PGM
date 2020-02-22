package tc.oc.pgm.api.party.event;

import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.party.Competitor;

/**
 * Called when a {@link Competitor} is removed from the {@link Match}.
 *
 * @see PartyRemoveEvent
 */
public class CompetitorRemoveEvent extends PartyRemoveEvent {

  public CompetitorRemoveEvent(Competitor competitor) {
    super(competitor);
  }

  /**
   * Get the {@link Competitor} for the {@link CompetitorAddEvent}.
   *
   * @return The {@link Competitor}.
   */
  public final Competitor getCompetitor() {
    return (Competitor) getParty();
  }
}
