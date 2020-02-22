package tc.oc.pgm.projectile;

import org.bukkit.entity.Entity;
import org.bukkit.event.Cancellable;
import org.bukkit.event.EntityAction;
import org.bukkit.event.HandlerList;
import org.bukkit.event.entity.EntityEvent;
import org.bukkit.projectiles.ProjectileSource;

/**
 * Called when a projectile is launched. Difference from the Bukkit version is that the projectile
 * can be any Entity, not necessarily a Projectile.
 */
public class EntityLaunchEvent extends EntityEvent implements Cancellable, EntityAction {

  private final ProjectileSource source;
  private boolean cancelled;

  public EntityLaunchEvent(Entity launched, ProjectileSource source) {
    super(launched);
    this.source = source;
  }

  public ProjectileSource getSource() {
    return source;
  }

  @Override
  public Entity getActor() {
    return source instanceof Entity ? (Entity) source : null;
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
