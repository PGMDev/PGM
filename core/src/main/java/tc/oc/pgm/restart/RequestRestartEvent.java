package tc.oc.pgm.restart;

import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.bukkit.plugin.Plugin;

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
      RestartManager.removeDeferral(this);
    }

    public boolean isDeferring() {
      return RestartManager.isDeferredBy(this);
    }
  }

  private static final HandlerList handlers = new HandlerList();

  public Deferral defer(Plugin plugin) {
    Deferral deferral = new Deferral(plugin);
    RestartManager.addDeferral(deferral);
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
