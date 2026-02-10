package tc.oc.pgm.restart;

import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.bukkit.plugin.Plugin;

public class RequestRestartEvent extends Event {

  public record Deferral(Plugin plugin) {
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

    @Deprecated
    public Plugin getPlugin() {
      return plugin();
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
