package tc.oc.pgm.util.event.entity;

import org.bukkit.entity.Entity;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import tc.oc.pgm.util.event.SportPaper;

/** Called when a burning entity is extinguished. */
@SportPaper
public class EntityExtinguishEvent extends Event {

  private final Entity entity;

  public EntityExtinguishEvent(Entity entity) {
    this.entity = entity;
  }

  public Entity getEntity() {
    return entity;
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
