package tc.oc.pgm.api.event;

import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import tc.oc.pgm.restart.RestartTask;

/**
 * Fired by {@link RestartTask} when a requested restart is cancelled. Any registered {@link
 * RequestRestartEvent.Deferral}s are forgotten by the manager and can be discarded.
 */
public class CancelRestartEvent extends Event {

  private static final HandlerList handlers = new HandlerList();

  @Override
  public HandlerList getHandlers() {
    return handlers;
  }

  public static HandlerList getHandlerList() {
    return handlers;
  }
}
