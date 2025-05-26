package tc.oc.pgm.util.event.entity;

import org.bukkit.entity.Entity;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import tc.oc.pgm.util.event.SportPaper;

/**
 * This event is called when an {@link Entity} is removed from the world because it has fallen 64
 * blocks into the void.
 */
@SportPaper
public class EntityDespawnInVoidEvent extends Event {
  private final Entity entity;
  private static final HandlerList handlers = new HandlerList();

  public EntityDespawnInVoidEvent(Entity entity) {
    this.entity = entity;
  }

  public Entity getEntity() {
    return entity;
  }

  public static HandlerList getHandlerList() {
    return handlers;
  }

  @Override
  public HandlerList getHandlers() {
    return handlers;
  }
}
