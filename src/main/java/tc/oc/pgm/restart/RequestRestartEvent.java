package tc.oc.pgm.restart;

import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.bukkit.plugin.Plugin;

/**
 * Fired by {@link RestartManager} when a server restart is requested. The server will normally
 * restart immediately after the event returns. To defer the restart, call {@link #defer} to get a
 * {@link Deferral} object, then call {@link Deferral#resume} on that at some future time to resume
 * the restart.
 */
public class RequestRestartEvent extends Event {

  public class Deferral {
    private final Plugin plugin;

    public Deferral(Plugin plugin) {
      this.plugin = plugin;
    }

    public Plugin getPlugin() {
      return plugin;
    }

    /**
     * Allow the deferred restart to proceed. After this method is called, this object becomes
     * useless and can be discarded.
     */
    public void resume() {
      restartManager.resumeRestart(this);
    }

    public boolean isDeferring() {
      return restartManager.isRestartDeferredBy(this);
    }
  }

  private final RestartManager restartManager;

  public RequestRestartEvent(RestartManager restartManager) {
    this.restartManager = restartManager;
  }

  public Deferral defer(Plugin plugin) {
    Deferral deferral = new Deferral(plugin);
    this.restartManager.deferRestart(deferral);
    return deferral;
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
