package tc.oc.pgm.spawns.events;

import org.bukkit.Location;
import org.bukkit.event.HandlerList;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.api.player.event.MatchPlayerEvent;

public class PlayerPostSpawnEvent extends MatchPlayerEvent {
  protected final Location location;

  public PlayerPostSpawnEvent(MatchPlayer player, Location location) {
    super(player);
    this.location = location;
  }

  /** Gets the location this player spawned at. */
  public Location getLocation() {
    return this.location;
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
