package tc.oc.pgm.community.events;

import org.bukkit.event.HandlerList;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.api.player.event.MatchPlayerEvent;

public class PlayerVanishEvent extends MatchPlayerEvent {

  private final boolean vanished;

  public PlayerVanishEvent(MatchPlayer vanisher, boolean vanished) {
    super(vanisher);
    this.vanished = vanished;
  }

  public boolean isVanished() {
    return vanished;
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
