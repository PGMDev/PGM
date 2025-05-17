package tc.oc.pgm.restart;

import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.bukkit.plugin.Plugin;
import tc.oc.pgm.countdowns.Countdown;

public class RequestRestartEvent extends Event {

  private Countdown restartCountdown;

  public Countdown getRestartCountdown() {
    return restartCountdown;
  }

  public void setRestartCountdown(Countdown restartCountdown) {
    this.restartCountdown = restartCountdown;
  }

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
      RestartManager.getInstance().removeDeferral(this);
    }

    public boolean isDeferring() {
      return RestartManager.getInstance().isDeferredBy(this);
    }
  }

  public Deferral defer(Plugin plugin) {
    Deferral deferral = new Deferral(plugin);
    RestartManager.getInstance().addDeferral(deferral);
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
