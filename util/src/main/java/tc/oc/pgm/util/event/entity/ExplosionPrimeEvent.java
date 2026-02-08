package tc.oc.pgm.util.event.entity;

import lombok.Getter;
import lombok.Setter;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Explosive;
import org.bukkit.event.Cancellable;
import org.bukkit.event.HandlerList;
import org.bukkit.event.entity.EntityEvent;

public class ExplosionPrimeEvent extends EntityEvent implements Cancellable {
  private static final HandlerList handlers = new HandlerList();

  private boolean cancel;

  @Getter
  @Setter
  private float radius;

  @Setter
  private boolean fire;

  public ExplosionPrimeEvent(Entity what, float radius, boolean fire) {
    super(what);
    this.cancel = false;
    this.radius = radius;
    this.fire = fire;
  }

  public ExplosionPrimeEvent(Explosive explosive) {
    this(explosive, explosive.getYield(), explosive.isIncendiary());
  }

  public boolean isCancelled() {
    return this.cancel;
  }

  public void setCancelled(boolean cancel) {
    this.cancel = cancel;
  }

  public boolean getFire() {
    return this.fire;
  }

  public HandlerList getHandlers() {
    return handlers;
  }

  public static HandlerList getHandlerList() {
    return handlers;
  }
}
