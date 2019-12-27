package tc.oc.pgm.api.event;

import javax.annotation.Nullable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.bukkit.plugin.Plugin;
import org.joda.time.Duration;
import tc.oc.pgm.restart.RestartTask;

/**
 * Fired by {@link RestartTask} when a server restart is requested. The server will normally restart
 * immediately after the event returns. To defer the restart, call {@link #defer} to get a {@link
 * Deferral} object, then call {@link Deferral#remove} to remove the deferral at a later time.
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
     * Remove this deferral from blocking the Restart Task. After this method is called, this object
     * becomes useless and can be discarded.
     */
    public void remove() {
      restartTask.removeDeferral(this);
    }

    public boolean isDeferring() {
      return restartTask.isDeferredBy(this);
    }
  }

  private final RestartTask restartTask;

  public RequestRestartEvent(RestartTask restartTask) {
    this.restartTask = restartTask;
  }

  public Deferral defer(Plugin plugin) {
    Deferral deferral = new Deferral(plugin);
    this.restartTask.addDeferral(deferral);
    return deferral;
  }

  public @Nullable Duration getDelayDuration() {
    return this.restartTask.getDelayDuration();
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
