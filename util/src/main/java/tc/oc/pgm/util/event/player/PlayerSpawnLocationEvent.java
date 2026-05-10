package tc.oc.pgm.util.event.player;

import org.bukkit.Location;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jspecify.annotations.NullMarked;
import tc.oc.pgm.util.platform.Platform;

@NullMarked
public class PlayerSpawnLocationEvent extends Event {

  private static final HandlerList HANDLER_LIST = new HandlerList();

  private Location spawnLocation;

  public PlayerSpawnLocationEvent(final Location spawnLocation) {
    super(Platform.isModern());
    this.spawnLocation = spawnLocation.clone();
  }

  public Location getSpawnLocation() {
    return this.spawnLocation.clone();
  }

  public void setSpawnLocation(final Location location) {
    this.spawnLocation = location.clone();
  }

  @Override
  public HandlerList getHandlers() {
    return HANDLER_LIST;
  }

  public static HandlerList getHandlerList() {
    return HANDLER_LIST;
  }
}
