package tc.oc.pgm.events;

import org.bukkit.event.HandlerList;
import tc.oc.pgm.api.party.Party;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.join.JoinRequest;

/** Called BEFORE a {@link MatchPlayer} leaves a {@link Party} */
public class PlayerLeavePartyEvent extends PlayerPartyChangeEventBase {

  public PlayerLeavePartyEvent(MatchPlayer player, Party oldParty, JoinRequest joinRequest) {
    super(player, oldParty, null, joinRequest);
  }

  public Party getParty() {
    return oldParty;
  }

  private static final HandlerList handlers = new HandlerList();

  @Override
  public HandlerList getHandlers() {
    return handlers;
  }

  public static HandlerList getHandlerList() {
    return handlers;
  }
}
