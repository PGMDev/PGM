package tc.oc.pgm.community.events;

import org.bukkit.event.HandlerList;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.api.player.event.MatchPlayerEvent;

/** PlayerVanishEvent - Called when a player's vanish status changes * */
public class PlayerVanishEvent extends MatchPlayerEvent {

  public PlayerVanishEvent(MatchPlayer vanisher, boolean vanished) {
    super(vanisher);
  }

  public boolean isVanished() {
    return getPlayer().isVanished();
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
