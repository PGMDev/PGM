package tc.oc.pgm.api.event;

import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.bukkit.plugin.Plugin;
import tc.oc.pgm.restart.RestartManager;

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
     * Remove the deferral from blocking the restart. After this method is called, object is useless
     * and can be discarded.
     */
    public void remove() {
      restartManager.removeDeferral(this);
    }

    public boolean isDeferring() {
      return restartManager.isDeferredBy(this);
    }
  }

  private static final HandlerList handlers = new HandlerList();
  private final RestartManager restartManager;

  public RequestRestartEvent(RestartManager restartManager) {
    this.restartManager = restartManager;
  }

  public Deferral defer(Plugin plugin) {
    Deferral deferral = new Deferral(plugin);
    this.restartManager.addDeferral(deferral);
    return deferral;
  }

  @Override
  public HandlerList getHandlers() {
    return handlers;
  }

  public static HandlerList getHandlerList() {
    return handlers;
  }
}
