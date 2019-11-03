package tc.oc.pgm.api.party.event;

import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.party.Competitor;

/**
 * Called when a {@link Competitor} is added to the {@link Match}.
 *
 * @see PartyAddEvent
 */
public class CompetitorAddEvent extends PartyAddEvent {

  public CompetitorAddEvent(Competitor competitor) {
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
