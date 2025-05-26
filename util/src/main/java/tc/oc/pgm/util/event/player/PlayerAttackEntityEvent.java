package tc.oc.pgm.util.event.player;

import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import tc.oc.pgm.util.event.SportPaper;

/**
 * Called when a player left-clicks on any entity. This is called before any other event, and
 * cancelling it prevents all further effects of the right-click.
 */
@SportPaper
public class PlayerAttackEntityEvent extends Event implements Cancellable {
  private static final HandlerList handlers = new HandlerList();
  private final Player player;
  protected Entity clickedEntity;
  boolean cancelled = false;

  public PlayerAttackEntityEvent(final Player player, final Entity clickedEntity) {
    this.player = player;
    this.clickedEntity = clickedEntity;
  }

  public Player getPlayer() {
    return player;
  }

  public boolean isCancelled() {
    return cancelled;
  }

  public void setCancelled(boolean cancel) {
    this.cancelled = cancel;
  }

  /**
   * Gets the entity that was left-clicked by the player.
   *
   * @return entity left-clicked by player
   */
  public Entity getLeftClicked() {
    return this.clickedEntity;
  }

  @Override
  public HandlerList getHandlers() {
    return handlers;
  }

  public static HandlerList getHandlerList() {
    return handlers;
  }
}
