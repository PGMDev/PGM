package tc.oc.pgm.rotation.vote.events;

import org.bukkit.event.HandlerList;
import tc.oc.pgm.api.map.MapInfo;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.api.player.event.MatchPlayerEvent;

public class MatchPlayerVoteEvent extends MatchPlayerEvent {

  private final MapInfo map;
  private final boolean add;

  public MatchPlayerVoteEvent(MatchPlayer player, MapInfo map, boolean add) {
    super(player);
    this.map = map;
    this.add = add;
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
