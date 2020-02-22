package tc.oc.pgm.events;

import tc.oc.pgm.api.party.Competitor;
import tc.oc.pgm.api.player.MatchPlayer;

/**
 * Called immediately before a player leaves a {@link Competitor}. This differs from {@link
 * PlayerPartyChangeEvent} in a few ways:
 *
 * <p>- It is only called when leaving a party, and only when that party is participating - It is
 * called before the change - It can be cancelled
 *
 * <p>If cancellation is not required, {@link PlayerPartyChangeEvent} should be used instead.
 */
public class PlayerParticipationStopEvent extends PlayerParticipationEvent {
  public PlayerParticipationStopEvent(MatchPlayer player, Competitor competitor) {
    super(player, competitor);
  }
}
