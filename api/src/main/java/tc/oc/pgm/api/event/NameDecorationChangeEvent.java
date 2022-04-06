package tc.oc.pgm.api.event;

import java.util.UUID;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

/**
 * Event to be thrown in order for the name to be re-rendered, asking the name decoration provider
 * what prefix & suffix to use
 */
public class NameDecorationChangeEvent extends Event {

  private final UUID uuid;

  public NameDecorationChangeEvent(UUID uuid) {
    this.uuid = uuid;
  }

  public UUID getUUID() {
    return uuid;
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
