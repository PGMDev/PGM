package tc.oc.pgm.projectile;

import lombok.Getter;
import lombok.Setter;
import org.bukkit.entity.Entity;
import org.bukkit.event.Cancellable;
import org.bukkit.event.HandlerList;
import org.bukkit.event.entity.EntityEvent;
import org.bukkit.projectiles.ProjectileSource;

/**
 * Called when a projectile is launched. Difference from the Bukkit version is that the projectile
 * can be any Entity, not necessarily a Projectile.
 */
@Getter
public class EntityLaunchEvent extends EntityEvent implements Cancellable {

  private final ProjectileSource source;

  @Setter
  private boolean cancelled;

  public EntityLaunchEvent(Entity launched, ProjectileSource source) {
    super(launched);
    this.source = source;
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
