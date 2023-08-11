package tc.oc.pgm.events;

import org.jetbrains.annotations.Nullable;
import tc.oc.pgm.api.party.Competitor;
import tc.oc.pgm.api.party.Party;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.join.JoinRequest;

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

  private final @Nullable Party nextParty;

  public PlayerParticipationStopEvent(
      MatchPlayer player, Competitor competitor, JoinRequest request, @Nullable Party nextParty) {
    super(player, competitor, request);
    this.nextParty = nextParty;
  }

  /**
   * The next party the player will try to join after ending this participation.
   *
   * @return the next party, or null if the player is disconnecting
   */
  public @Nullable Party getNextParty() {
    return nextParty;
  }
}
