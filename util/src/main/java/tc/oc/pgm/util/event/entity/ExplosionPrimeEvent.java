package tc.oc.pgm.util.event.entity;

import org.bukkit.entity.Entity;
import org.bukkit.entity.Explosive;
import org.bukkit.event.Cancellable;
import org.bukkit.event.HandlerList;
import org.bukkit.event.entity.EntityEvent;

public class ExplosionPrimeEvent extends EntityEvent implements Cancellable {
  private static final HandlerList handlers = new HandlerList();

  private boolean cancel;
  private float radius;
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

  public float getRadius() {
    return this.radius;
  }

  public void setRadius(float radius) {
    this.radius = radius;
  }

  public boolean getFire() {
    return this.fire;
  }

  public void setFire(boolean fire) {
    this.fire = fire;
  }

  public HandlerList getHandlers() {
    return handlers;
  }

  public static HandlerList getHandlerList() {
    return handlers;
  }
}
