package tc.oc.pgm.events;

import static tc.oc.pgm.util.Assert.assertNotNull;

import org.bukkit.event.HandlerList;
import tc.oc.pgm.api.party.Party;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.api.player.event.MatchPlayerEvent;

/** Called BEFORE a {@link MatchPlayer} leaves a {@link Party} */
public class PlayerLeavePartyEvent extends MatchPlayerEvent {

  protected final Party oldParty;

  public PlayerLeavePartyEvent(MatchPlayer player, Party oldParty) {
    super(player);
    this.oldParty = assertNotNull(oldParty);
  }

  public Party getParty() {
    return oldParty;
  }

  private static HandlerList handlers = new HandlerList();

  @Override
  public HandlerList getHandlers() {
    return handlers;
  }

  public static HandlerList getHandlerList() {
    return handlers;
  }
}
