package tc.oc.pgm.api.match.event;

import org.bukkit.event.HandlerList;
import org.jspecify.annotations.Nullable;
import tc.oc.pgm.api.map.MapInfo;
import tc.oc.pgm.api.match.Match;

public class MatchVoteFinishEvent extends MatchEvent {

  private final @Nullable MapInfo pickedMap;

  public MatchVoteFinishEvent(Match match, @Nullable MapInfo pickedMap) {
    super(match);
    this.pickedMap = pickedMap;
  }

  @Nullable
  public MapInfo getPickedMap() {
    return pickedMap;
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
