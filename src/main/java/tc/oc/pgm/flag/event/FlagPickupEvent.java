package tc.oc.pgm.flag.event;

import org.bukkit.Location;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.flag.Flag;

/**
 * Fired BEFORE a player picks up a flag, allowing it to be cancelled. This is used by {@link
 * tc.oc.pgm.regions.RegionMatchModule} to prevent flags from being picked up inside regions that
 * the player would not be allowed to enter if they were already carrying it.
 */
public class FlagPickupEvent extends Event implements Cancellable {

  private final Flag flag;
  private final MatchPlayer carrier;
  private final Location location;
  private boolean cancelled;

  public FlagPickupEvent(Flag flag, MatchPlayer carrier, Location location) {
    this.flag = flag;
    this.carrier = carrier;
    this.location = location;
  }

  public Flag getFlag() {
    return flag;
  }

  public MatchPlayer getCarrier() {
    return carrier;
  }

  public Location getLocation() {
    return location;
  }

  @Override
  public boolean isCancelled() {
    return this.cancelled;
  }

  @Override
  public void setCancelled(boolean yes) {
    this.cancelled = yes;
  }

  private static final HandlerList handlers = new HandlerList();

  public static HandlerList getHandlerList() {
    return handlers;
  }

  @Override
  public HandlerList getHandlers() {
    return handlers;
  }
}
