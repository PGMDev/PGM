package tc.oc.pgm.events;

import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import tc.oc.pgm.api.map.MapInfo;

public class MapVoteWinnerEvent extends Event {

  private MapInfo map;

  public MapVoteWinnerEvent(MapInfo map) {
    this.map = map;
  }

  public MapInfo getMap() {
    return map;
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
