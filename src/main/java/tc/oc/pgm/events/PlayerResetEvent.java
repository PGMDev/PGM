package tc.oc.pgm.events;

import org.bukkit.event.HandlerList;
import tc.oc.pgm.api.match.event.MatchEvent;
import tc.oc.pgm.api.player.MatchPlayer;

/** Called immediately before a MatchPlayer is "reset" i.e. set to the default state */
public class PlayerResetEvent extends MatchEvent {
  private final MatchPlayer player;

  public PlayerResetEvent(MatchPlayer player) {
    super(player.getMatch());
    this.player = player;
  }

  public MatchPlayer getPlayer() {
    return player;
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
