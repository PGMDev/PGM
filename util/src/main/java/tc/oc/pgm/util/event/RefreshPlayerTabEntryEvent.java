package tc.oc.pgm.util.event;

import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.bukkit.event.player.PlayerEvent;

public class RefreshPlayerTabEntryEvent extends PlayerEvent {

  /**
   * Allow third-party plugins to request a player tablist entry be refreshed. Useful for nickname
   * or friend status changes
   *
   * @param who The {@link Player}
   */
  public RefreshPlayerTabEntryEvent(Player who) {
    super(who);
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
