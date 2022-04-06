package tc.oc.pgm.util.event;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.bukkit.event.player.PlayerMoveEvent;
import tc.oc.pgm.api.event.GeneralizedEvent;
import tc.oc.pgm.util.block.BlockVectors;

/**
 * An event when a {@link Player} crosses a block boundary.
 *
 * <p>If {@link #isCancelled()} is {@code true}, the player's location is reset to {@link
 * #getBlockFrom()} with adjustments to ensure they are standing on a surface.
 */
public class PlayerCoarseMoveEvent extends GeneralizedEvent {

  private final Location from;
  private Location to;

  public PlayerCoarseMoveEvent(final PlayerMoveEvent cause) {
    super(cause);
    this.from = BlockVectors.center(cause.getFrom());
    this.to = BlockVectors.center(cause.getTo());
  }

  @Override
  public PlayerMoveEvent getCause() {
    return (PlayerMoveEvent) super.getCause();
  }

  /**
   * Gets the {@link Player} that moved.
   *
   * @return a player
   */
  public Player getPlayer() {
    return this.getCause().getPlayer();
  }

  /**
   * Gets the {@link Location} where the player originated.
   *
   * @return a location
   */
  public Location getFrom() {
    return this.getCause().getFrom();
  }

  /**
   * Gets the block {@link Location} where the player originated.
   *
   * @return a block location
   */
  public Location getBlockFrom() {
    return this.from;
  }

  /**
   * Gets the {@link Location} where the player moved.
   *
   * @return a location
   */
  public Location getTo() {
    return this.getCause().getTo();
  }

  /**
   * Gets the block {@link Location} where the player moved.
   *
   * @return a block location
   */
  public Location getBlockTo() {
    return this.to;
  }

  /**
   * Sets the {@link Location} where the player will be moved.
   *
   * @param location a new location
   */
  public void setTo(final Location location) {
    this.getCause().setTo(location);
    this.to = BlockVectors.center(location);
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
