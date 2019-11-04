package tc.oc.pgm.events;

import tc.oc.pgm.api.party.Competitor;
import tc.oc.pgm.api.player.MatchPlayer;

/**
 * Called immediately before a player joins a {@link Competitor}. This differs from {@link
 * PlayerPartyChangeEvent} in a few ways:
 *
 * <p>- It is only called when joining a party, and only when that party is participating - It is
 * called before the change - It can be cancelled
 *
 * <p>If cancellation is not required, {@link PlayerPartyChangeEvent} should be used instead.
 */
public class PlayerParticipationStartEvent extends PlayerParticipationEvent {
  public PlayerParticipationStartEvent(MatchPlayer player, Competitor competitor) {
    super(player, competitor);
  }
}
