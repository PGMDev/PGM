package tc.oc.pgm.community.events;

import org.bukkit.event.HandlerList;
import tc.oc.pgm.api.event.ExtendedCancellable;
import tc.oc.pgm.api.player.MatchPlayer;

public class PlayerVanishEvent extends ExtendedCancellable {

  private MatchPlayer vanisher;
  private boolean vanished;

  public PlayerVanishEvent(MatchPlayer vanisher, boolean vanished) {
    this.vanisher = vanisher;
    this.vanished = vanished;
  }

  public MatchPlayer getPlayer() {
    return vanisher;
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
