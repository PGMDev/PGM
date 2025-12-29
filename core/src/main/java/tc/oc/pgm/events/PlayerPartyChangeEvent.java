package tc.oc.pgm.events;

import static tc.oc.pgm.util.Assert.assertTrue;

import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.Nullable;
import tc.oc.pgm.api.party.Party;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.join.JoinRequest;

/**
 * Called AFTER a player leaves and/or joins a party. Subclasses are called for more specific cases,
 * and those should be used whenever possible.
 *
 * <p>If the player is leaving the match, they will have already been removed when this event is
 * called, and their party will be set to null.
 */
public class PlayerPartyChangeEvent extends PlayerPartyChangeEventBase {

  public PlayerPartyChangeEvent(
      MatchPlayer player, @Nullable Party oldParty, @Nullable Party newParty, JoinRequest request) {
    super(player, oldParty, newParty, request);
    assertTrue(oldParty != newParty);
  }

  private static final HandlerList handlers = new HandlerList();

  public static HandlerList getHandlerList() {
    return handlers;
  }

  @Override
  public HandlerList getHandlers() {
    return handlers;
  }
}
