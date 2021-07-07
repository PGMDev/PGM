package tc.oc.pgm.api.event;

import org.bukkit.event.HandlerList;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.api.player.event.MatchPlayerEvent;

/** PlayerVanishEvent - Called when a player's vanish status changes * */
public class PlayerVanishEvent extends MatchPlayerEvent {

  private final boolean vanish;
  private final boolean quiet;

  public PlayerVanishEvent(MatchPlayer vanisher, boolean vanished, boolean quiet) {
    super(vanisher);
    this.vanish = vanished;
    this.quiet = quiet;
  }

  public boolean isVanished() {
    return vanish;
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
