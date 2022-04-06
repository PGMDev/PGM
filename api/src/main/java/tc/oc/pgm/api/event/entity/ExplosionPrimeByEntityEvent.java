package tc.oc.pgm.api.event.entity;

import static com.google.common.base.Preconditions.checkNotNull;

import org.bukkit.entity.Entity;
import org.bukkit.entity.Explosive;
import org.bukkit.event.entity.ExplosionPrimeEvent;
import tc.oc.pgm.api.event.SportPaper;

/**
 * Called when an entity has made another entity decide to explode, specifically when: - a player
 * activates a TNT block or Creeper with Flint & Steel - an entity's explosion chains to a TNT block
 * - a flaming arrow activates a TNT block - an entity damages an Ender Crystal
 */
@SportPaper
public class ExplosionPrimeByEntityEvent extends ExplosionPrimeEvent {

  private final Entity primer;

  public ExplosionPrimeByEntityEvent(Entity what, float radius, boolean fire, Entity primer) {
    super(what, radius, fire);
    this.primer = checkNotNull(primer);
  }

  public ExplosionPrimeByEntityEvent(Explosive explosive, Entity primer) {
    this(explosive, explosive.getYield(), explosive.isIncendiary(), primer);
  }

  /** @return The {@link Entity} that caused this entity to become primed */
  public Entity getPrimer() {
    return primer;
  }
}
