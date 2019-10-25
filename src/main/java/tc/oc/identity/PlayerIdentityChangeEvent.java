package tc.oc.identity;

import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.bukkit.event.player.PlayerEvent;

/**
 * Fired when a player changes/clears their nickname immediately during a session, i.e. with "/nick
 * -i". This is NEVER fired on login or logout.
 */
public class PlayerIdentityChangeEvent extends PlayerEvent {
  private final Identity oldIdentity;
  private final Identity newIdentity;

  public PlayerIdentityChangeEvent(Player player, Identity oldIdentity, Identity newIdentity) {
    super(player);
    this.oldIdentity = oldIdentity;
    this.newIdentity = newIdentity;
  }

  public Identity getOldIdentity() {
    return oldIdentity;
  }

  public Identity getNewIdentity() {
    return newIdentity;
  }

  /** HandlerList stuff */
  private static final HandlerList handlers = new HandlerList();

  @Override
  public HandlerList getHandlers() {
    return handlers;
  }

  public static HandlerList getHandlerList() {
    return handlers;
  }
}
