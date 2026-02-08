package tc.oc.pgm.spawns.events;

import lombok.Getter;
import lombok.Setter;
import org.bukkit.Location;
import org.bukkit.event.HandlerList;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.api.player.event.MatchPlayerEvent;

@Getter
@Setter
public class PlayerSpawnEvent extends MatchPlayerEvent {

  /** The location where this player will spawn at. */
  protected Location location;

  public PlayerSpawnEvent(MatchPlayer player, Location location) {
    super(player);
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
