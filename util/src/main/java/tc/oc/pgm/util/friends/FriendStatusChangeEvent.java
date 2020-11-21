package tc.oc.pgm.util.friends;

import java.util.UUID;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

/** Called when friendship status is updated for a player * */
public class FriendStatusChangeEvent extends Event {

  private UUID playerId;

  public FriendStatusChangeEvent(UUID playerId) {
    this.playerId = playerId;
  }

  public UUID getPlayerId() {
    return playerId;
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
