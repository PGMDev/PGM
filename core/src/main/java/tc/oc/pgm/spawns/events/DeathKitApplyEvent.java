package tc.oc.pgm.spawns.events;

import org.bukkit.event.HandlerList;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.api.player.event.MatchPlayerEvent;

/**
 * Called shortly after a participating player dies and the time is right to give them any items
 * that they can use while on the death screen.
 */
public class DeathKitApplyEvent extends MatchPlayerEvent {

  public DeathKitApplyEvent(MatchPlayer player) {
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
