package tc.oc.pgm.events;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.bukkit.event.player.PlayerMoveEvent;
import tc.oc.block.BlockVectors;
import tc.oc.pgm.api.event.GeneralizingEvent;

/**
 * Wraps PlayerMoveEvents that cross block boundaries. The from and to locations are the same as the
 * wrapped event, so the locations for consecutive coarse events will not generally connect to each
 * other.
 *
 * <p>Cancelling a coarse event results in the player's position being reset to the center of the
 * block at the from location, with some adjustments to Y to try and place them on the surface of
 * the block.
 */
public class CoarsePlayerMoveEvent extends GeneralizingEvent {
  private static final HandlerList handlers = new HandlerList();

  protected final Player player;
  protected final Location from;
  protected Location to;

  public CoarsePlayerMoveEvent(Event cause, Player player, Location from, Location to) {
    super(cause);
    this.player = player;
    this.from = from;
    this.to = to;
  }

  public Player getPlayer() {
    return this.player;
  }

  @Override
  public Player getActor() {
    return getPlayer();
  }

  public Location getFrom() {
    return this.from;
  }

  public Location getBlockFrom() {
    return BlockVectors.center(this.from);
  }

  public Location getTo() {
    return this.to;
  }

  public Location getBlockTo() {
    return BlockVectors.center(this.to);
  }

  public void setTo(Location newLoc) {
    if (getCause() instanceof PlayerMoveEvent) {
      ((PlayerMoveEvent) getCause()).setTo(newLoc);
    }
  }

  @Override
  public HandlerList getHandlers() {
    return handlers;
  }

  public static HandlerList getHandlerList() {
    return handlers;
  }
}
