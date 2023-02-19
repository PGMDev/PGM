package tc.oc.pgm.events;

import static tc.oc.pgm.util.Assert.assertNotNull;

import com.google.common.collect.Lists;
import java.util.List;
import net.kyori.adventure.text.Component;
import tc.oc.pgm.api.party.Party;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.api.player.event.MatchPlayerAddEvent;
import tc.oc.pgm.join.JoinRequest;

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

  private List<Component> extraLines = Lists.newArrayList();

  public PlayerJoinMatchEvent(MatchPlayer player, Party newParty, JoinRequest request) {
    super(player, null, assertNotNull(newParty), request);
  }

  public List<Component> getExtraLines() {
    return extraLines;
  }
}
