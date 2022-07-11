package tc.oc.pgm.events;

import org.bukkit.entity.Item;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public class ItemRemovedByActionEvent extends Event {
  private Item entity;

  public ItemRemovedByActionEvent(Item entity) {
    this.entity = entity;
  }

  public Item getEntity() {
    return entity;
  }

  private static final HandlerList handlers = new HandlerList();

  public static HandlerList getHandlerList() {
    return handlers;
  }

  @Override
  public HandlerList getHandlers() {
    return handlers;
  }
}
