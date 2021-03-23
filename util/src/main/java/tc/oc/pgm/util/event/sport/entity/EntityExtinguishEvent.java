package tc.oc.pgm.util.event.sport.entity;

import org.bukkit.entity.Entity;
import org.bukkit.event.HandlerList;
import org.bukkit.event.entity.EntityEvent;

/** Called when a burning entity is extinguished. */
public class EntityExtinguishEvent extends EntityEvent {

  public EntityExtinguishEvent(Entity combustee) {
    super(combustee);
  }

  @Override
  public HandlerList getHandlers() {
    return handlers;
  }

  private static final HandlerList handlers = new HandlerList();

  public static HandlerList getHandlerList() {
    return handlers;
  }
}
