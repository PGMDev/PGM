package tc.oc.pgm.util.event.entity;

import static tc.oc.pgm.util.Assert.assertNotNull;

import org.bukkit.entity.Entity;
import org.bukkit.entity.Explosive;
import tc.oc.pgm.util.event.SportPaper;

/**
 * Called when an entity has made another entity decide to explode, specifically when:<br>
 * - a player activates a TNT block or Creeper with Flint & Steel <br>
 * - an entity's explosion chains to a TNT block <br>
 * - a flaming arrow activates a TNT block <br>
 * - an entity damages an Ender Crystal
 */
@SportPaper
public class ExplosionPrimeByEntityEvent extends ExplosionPrimeEvent {
  private final Entity primer;

  public ExplosionPrimeByEntityEvent(Entity what, float radius, boolean fire, Entity primer) {
    super(what, radius, fire);
    this.primer = assertNotNull(primer);
  }

  public ExplosionPrimeByEntityEvent(Explosive explosive, Entity primer) {
    this(explosive, explosive.getYield(), explosive.isIncendiary(), primer);
  }

  /** @return The {@link Entity} that caused this entity to become primed */
  public Entity getPrimer() {
    return primer;
  }
}
