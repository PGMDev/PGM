package tc.oc.pgm.api.player.event;

import org.bukkit.event.HandlerList;
import tc.oc.pgm.api.player.MatchPlayer;

/** PlayerVanishEvent - Called when a player's vanish status changes * */
public class PlayerVanishEvent extends MatchPlayerEvent {

  private final boolean quiet;
  private final boolean vanished;

  public PlayerVanishEvent(MatchPlayer vanisher, boolean vanished, boolean quiet) {
    super(vanisher);
    this.quiet = quiet;
    this.vanished = vanished;
  }

  public boolean isVanished() {
    return vanished;
  }

  public boolean isQuiet() {
    return quiet;
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
