package tc.oc.pgm.rotation.vote.events;

import java.util.UUID;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import tc.oc.pgm.api.map.MapInfo;

public class MapPollVoteEvent extends Event {

  private final UUID playerId;
  private final MapInfo map;
  private final boolean add;

  public MapPollVoteEvent(UUID playerId, MapInfo map, boolean add) {
    this.playerId = playerId;
    this.map = map;
    this.add = add;
  }

  public UUID getPlayerId() {
    return playerId;
  }

  public MapInfo getMap() {
    return map;
  }

  public boolean isAdd() {
    return add;
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
