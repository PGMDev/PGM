package tc.oc.pgm.util.event.player;

import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.HandlerList;
import org.bukkit.event.player.PlayerEvent;
import tc.oc.pgm.api.event.SportPaper;

/**
 * Called when a player left-clicks on any entity. This is called before any other event, and
 * cancelling it prevents all further effects of the right-click.
 */
@SportPaper
public class PlayerAttackEntityEvent extends PlayerEvent implements Cancellable {
  private static final HandlerList handlers = new HandlerList();
  protected Entity clickedEntity;
  boolean cancelled = false;

  public PlayerAttackEntityEvent(final Player who, final Entity clickedEntity) {
    super(who);
    this.clickedEntity = clickedEntity;
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
