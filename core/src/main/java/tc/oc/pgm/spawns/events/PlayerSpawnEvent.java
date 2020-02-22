package tc.oc.pgm.spawns.events;

import org.bukkit.Location;
import org.bukkit.event.HandlerList;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.api.player.event.MatchPlayerEvent;

public class PlayerSpawnEvent extends MatchPlayerEvent {

  protected Location location;

  public PlayerSpawnEvent(MatchPlayer player, Location location) {
    super(player);
    this.location = location;
  }

  /** Gets the location this player will spawn at. */
  public Location getLocation() {
    return this.location;
  }

  /** Sets the location where this player will spawn at. */
  public void setLocation(Location location) {
    this.location = location;
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
