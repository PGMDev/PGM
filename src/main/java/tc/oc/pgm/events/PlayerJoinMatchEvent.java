package tc.oc.pgm.events;

import static com.google.common.base.Preconditions.checkNotNull;

import tc.oc.pgm.api.party.Party;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.api.player.event.MatchPlayerAddEvent;

/**
 * Subclass of {@link PlayerJoinPartyEvent} called in cases where the player is joining the match
 * i.e. {@link #getOldParty()} is null.
 *
 * <p>This event is called at the finish of the joining process, after the player has joined the
 * initial {@link Party}. That party cannot be changed from within this event, nor can another party
 * change be executed.
 *
 * <p>A player's initial party can be changed from {@link MatchPlayerAddEvent}.
 */
public class PlayerJoinMatchEvent extends PlayerJoinPartyEvent {

  public PlayerJoinMatchEvent(MatchPlayer player, Party newParty) {
    super(player, null, checkNotNull(newParty));
  }
}
