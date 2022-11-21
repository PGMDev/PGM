package tc.oc.pgm.api.player.event;

import org.bukkit.event.HandlerList;
import tc.oc.pgm.api.player.MatchPlayer;

/** MatchPlayerFullJoinEvent - Called when a {@link MatchPlayer} attempts to join a full match * */
public class MatchPlayerFullJoinEvent extends MatchPlayerEvent {

  public MatchPlayerFullJoinEvent(MatchPlayer player) {
    super(player);
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
