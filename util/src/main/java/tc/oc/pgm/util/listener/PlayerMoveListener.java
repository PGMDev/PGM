package tc.oc.pgm.util.listener;

import java.util.Map;
import java.util.WeakHashMap;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.*;
import org.bukkit.event.player.PlayerTeleportEvent.TeleportCause;
import org.bukkit.util.Vector;
import tc.oc.pgm.api.Audience;
import tc.oc.pgm.util.block.BlockVectors;
import tc.oc.pgm.util.event.PlayerCoarseMoveEvent;

/** A listener that calls {@link PlayerCoarseMoveEvent}. */
public class PlayerMoveListener implements Listener {

  // The last location of a player that has been used to generate
  // coarse movement events. If a player is not in this list, then
  // the next movement event they generate can be assumed valid
  // on its own.
  private final Map<Player, Location> lastToLocation = new WeakHashMap<>();

  private void updateLastToLocation(Player player, Location location) {
    this.lastToLocation.put(player, location);
  }

  private void updateLastToLocation(final PlayerMoveEvent event) {
    if (event.isCancelled()) {
      this.updateLastToLocation(event.getPlayer(), event.getFrom());
    } else {
      this.updateLastToLocation(event.getPlayer(), event.getTo());
    }
  }

  @EventHandler(priority = EventPriority.HIGHEST)
  public void onPlayerMoveHigh(final PlayerMoveEvent event) {
    this.handleMovementHigh(event);
  }

  @EventHandler(priority = EventPriority.HIGHEST)
  public void onPlayerTeleportHigh(final PlayerTeleportEvent event) {
    this.handleMovementHigh(event);
  }

  private final void handleMovementHigh(final PlayerMoveEvent event) {
    Player player = event.getPlayer();
    Location originalFrom = event.getFrom();
    Location originalTo = event.getTo();

    Location oldTo = this.lastToLocation.get(player);
    if (oldTo != null && !oldTo.equals(originalFrom)) {
      // If this movement does not start where the last known movement ended,
      // we have to make up the missing movement. We do that by (potentially) firing
      // two coarse events for this one event, a "fake" one for the missing movement
      // and a "real" one for the current movement.

      // First, modify this event to look like the missing event, and fire
      // a coarse event that wraps it.
      event.setFrom(oldTo);
      event.setTo(originalFrom);
      this.updateLastToLocation(event);
      if (this.callCoarsePlayerMove(event)) {
        // If the fake coarse event was cancelled, we don't need to fire
        // the real one, so just return. Note that the wrapped event won't
        // actually be cancelled, rather its to location will be modified
        // to return the player to the oldTo location. Also note that if
        // the original event was already cancelled before the coarse event
        // fired, then we will never get here, and both the fake and real
        // events will go through.
        this.updateLastToLocation(event);
        return;
      }

      // Restore the event to its real state
      event.setFrom(originalFrom);
      event.setTo(originalTo);
    }

    this.updateLastToLocation(event);
    if (this.callCoarsePlayerMove(event)) {
      this.updateLastToLocation(event);
    }
  }

  /**
   * Fire a PlayerCoarseMoveEvent that wraps the given event, only if it crosses a block boundary
   *
   * @param event The movement event to potentially wrap
   * @return True if the original event was not cancelled, and a coarse event was fired, and that
   *     coarse event was cancelled. In this case, the wrapped event won't actually be cancelled,
   *     but callers should treat it like it is.
   */
  private boolean callCoarsePlayerMove(final PlayerMoveEvent event) {
    // Don't fire coarse events for teleports that are not "in-game"
    // e.g. /jumpto commands
    if (event instanceof PlayerTeleportEvent) {
      PlayerTeleportEvent teleportEvent = (PlayerTeleportEvent) event;
      if (teleportEvent.getCause() != TeleportCause.ENDER_PEARL
          && teleportEvent.getCause() != TeleportCause.UNKNOWN) {
        return false;
      }
    }

    // If the movement does not cross a block boundary, we don't care about it
    if (event.getTo().getBlock().equals(event.getFrom().getBlock())) {
      return false;
    }

    // Remember whether the original event was already cancelled
    boolean wasCancelled = event.isCancelled();

    PlayerCoarseMoveEvent generalEvent = new PlayerCoarseMoveEvent(event);
    callEvent(generalEvent);

    if (!wasCancelled && generalEvent.isCancelled()) {
      // When a coarse event is cancelled, we have our own logic for resetting the
      // player's position, so we un-cancel the event and instead modify its
      // to location to put the player where we want them.
      resetPosition(event);
      return true;
    } else {
      return false;
    }
  }

  /**
   * Modify the to location of the given event to prevent the movement and move the player so they
   * are standing on the center of the block at the from location.
   */
  private static void resetPosition(final PlayerMoveEvent event) {
    Location newLoc;
    double yValue = event.getFrom().getY();

    if (yValue <= 0 || event instanceof PlayerTeleportEvent) {
      newLoc = event.getFrom();
    } else {
      newLoc = BlockVectors.center(event.getFrom()).subtract(new Vector(0, 0.5, 0));
      if (newLoc.getBlock() != null) {
        switch (newLoc.getBlock().getType()) {
          case STEP:
          case WOOD_STEP:
            newLoc.add(new Vector(0, 0.5, 0));
            break;
          default:
            break;
        }
      }
    }

    newLoc.setPitch(event.getTo().getPitch());
    newLoc.setYaw(event.getTo().getYaw());
    event.setCancelled(false);
    event.setTo(newLoc);
  }

  // reset the last location on death
  @EventHandler(priority = EventPriority.MONITOR)
  public void onPlayerRespawn(final PlayerRespawnEvent event) {
    this.lastToLocation.remove(event.getPlayer());
  }

  @EventHandler(priority = EventPriority.MONITOR)
  public void onPlayerMoveMonitor(final PlayerMoveEvent event) {
    this.handleMovementMonitor(event);
  }

  @EventHandler(priority = EventPriority.MONITOR)
  public void onPlayerTeleportMonitor(final PlayerTeleportEvent event) {
    this.handleMovementMonitor(event);
  }

  private void handleMovementMonitor(PlayerMoveEvent event) {
    // It's possible for a PlayerMoveEvent to be modified by another
    // HIGHEST handler after we handle it, so we also check it at MONITOR
    this.updateLastToLocation(event);
  }

  @EventHandler(priority = EventPriority.MONITOR)
  public void processCancelMessage(final PlayerCoarseMoveEvent event) {
    if (event.isCancelled() && event.getCancellationReason() != null) {
      Audience.get(event.getPlayer()).sendWarning(event.getCancellationReason());
    }
  }

  private static void callEvent(final Event event) {
    Bukkit.getPluginManager().callEvent(event);
  }
}
