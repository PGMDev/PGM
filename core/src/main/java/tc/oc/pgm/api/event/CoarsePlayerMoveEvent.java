package tc.oc.pgm.api.event;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.bukkit.event.player.PlayerMoveEvent;
import tc.oc.pgm.util.block.BlockVectors;

/**
 * Wraps a {@link PlayerMoveEvent} when the {@link Player} crosses a {@link org.bukkit.block.Block}
 * boundary. The event's {@link #getFrom()} and {@link #getTo()} are identical to its underlying
 * {@link PlayerMoveEvent}.
 *
 * <p>When {@link #isCancelled()} is {@code true}, the {@link Player}'s position will be reset to
 * the center of the {@link org.bukkit.block.Block} at {@link #getFrom()}, with potential
 * adjustments to {@link Location#getY()} to ensure they are standing on the surface of the {@link
 * #getBlockFrom()}.
 */
public class CoarsePlayerMoveEvent extends GeneralizingEvent {

  private final Player player;
  private final Location from;
  private Location to;

  public CoarsePlayerMoveEvent(Event cause, Player player, Location from, Location to) {
    super(cause);
    this.player = player;
    this.from = from;
    this.to = to;
  }

  /**
   * Get the {@link Player} that moved.
   *
   * @return The {@link Player} that moved.
   */
  public final Player getPlayer() {
    return player;
  }

  /**
   * Get the exact {@link Location} the {@link Player} moved from.
   *
   * @return The previous {@link Location}.
   */
  public final Location getFrom() {
    return from;
  }

  /**
   * Get the {@link org.bukkit.block.Block} that is approximately at the center of {@link
   * #getFrom()}.
   *
   * @return The approximate center {@link org.bukkit.block.Block}.
   */
  public final Location getBlockFrom() {
    return BlockVectors.center(getFrom());
  }

  /**
   * Get the exact {@link Location} the {@link Player} moved to.
   *
   * @return The new and current {@link Location}.
   */
  public final Location getTo() {
    return to;
  }

  /**
   * Get the {@link org.bukkit.block.Block} that is approximately at the center of {@link #getTo()}.
   *
   * @return The approximate center {@link org.bukkit.block.Block}.
   */
  public final Location getBlockTo() {
    return BlockVectors.center(getTo());
  }

  /**
   * Change the {@link #getTo()} {@link Location} the {@link Player} will move to.
   *
   * @param newLoc The new {@link Location}.
   */
  public final void setTo(Location newLoc) {
    if (getCause() instanceof PlayerMoveEvent) {
      ((PlayerMoveEvent) getCause()).setTo(newLoc);
    }
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
