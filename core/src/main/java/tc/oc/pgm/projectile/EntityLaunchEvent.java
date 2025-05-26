package tc.oc.pgm.projectile;

import org.bukkit.entity.Entity;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.bukkit.projectiles.ProjectileSource;

/**
 * Called when a projectile is launched. Difference from the Bukkit version is that the projectile
 * can be any Entity, not necessarily a Projectile.
 */
public class EntityLaunchEvent extends Event implements Cancellable {

  private final Entity entity;
  private final ProjectileSource source;
  private boolean cancelled;

  public EntityLaunchEvent(Entity entity, ProjectileSource source) {
    this.entity = entity;
    this.source = source;
  }

  public Entity getEntity() {
    return entity;
  }

  public ProjectileSource getSource() {
    return source;
  }

  public boolean isCancelled() {
    return cancelled;
  }

  public void setCancelled(boolean cancel) {
    cancelled = cancel;
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
