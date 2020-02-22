package tc.oc.pgm.events;

import static com.google.common.base.Preconditions.checkNotNull;

import tc.oc.pgm.api.party.Party;
import tc.oc.pgm.api.player.MatchPlayer;

/**
 * Subclass of {@link PlayerLeavePartyEvent} called in cases where the player is leaving the match
 */
public class PlayerLeaveMatchEvent extends PlayerLeavePartyEvent {
  public PlayerLeaveMatchEvent(MatchPlayer player, Party party) {
    super(player, checkNotNull(party));
  }
}
