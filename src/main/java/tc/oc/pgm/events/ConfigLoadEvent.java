package tc.oc.pgm.events;

import org.bukkit.configuration.Configuration;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

/**
 * Fires after the server configuration loads/reloads, including when the plugin is first enabled.
 */
public class ConfigLoadEvent extends Event {
  private final Configuration config;

  public ConfigLoadEvent(Configuration config) {
    this.config = config;
  }

  public Configuration getConfig() {
    return config;
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
