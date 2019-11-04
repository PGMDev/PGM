package tc.oc.pgm.spawns.events;

import org.bukkit.event.HandlerList;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.api.player.event.MatchPlayerEvent;

/**
 * Called when a player becomes an observer, before any spawn kit is applied. Other modules can use
 * this event to give their own observer items.
 */
public class ObserverKitApplyEvent extends MatchPlayerEvent {
  public ObserverKitApplyEvent(MatchPlayer player) {
    super(player);
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
