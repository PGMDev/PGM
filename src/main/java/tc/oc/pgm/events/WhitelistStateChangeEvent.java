package tc.oc.pgm.events;

import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public class WhitelistStateChangeEvent extends Event {

  private final boolean enabled;

  public WhitelistStateChangeEvent(boolean enabled) {
    this.enabled = enabled;
  }

  public boolean isEnabled() {
    return enabled;
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
