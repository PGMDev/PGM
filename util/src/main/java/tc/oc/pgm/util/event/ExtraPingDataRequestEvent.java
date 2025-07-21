package tc.oc.pgm.util.event;

import com.google.gson.JsonObject;
import org.bukkit.event.HandlerList;
import org.bukkit.event.server.ServerEvent;
import org.bukkit.plugin.Plugin;

/**
 * Called by individual platform implementations to allow listeners to attach additional server ping
 * data
 */
public abstract class ExtraPingDataRequestEvent extends ServerEvent {
  public abstract JsonObject getServerListExtra(Plugin plugin);

  private static final HandlerList handlers = new HandlerList();

  @Override
  public HandlerList getHandlers() {
    return handlers;
  }

  public static HandlerList getHandlerList() {
    return handlers;
  }
}
