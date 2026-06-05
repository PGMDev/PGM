package tc.oc.pgm.action.actions;

import org.bukkit.Location;
import org.bukkit.util.Vector;
import org.jspecify.annotations.Nullable;
import tc.oc.pgm.api.feature.FeatureReference;
import tc.oc.pgm.api.filter.Filter;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.filters.Filterable;
import tc.oc.pgm.projectile.ProjectileDefinition;

public class LaunchProjectileAction<T extends Filterable<?>> extends AbstractAction<T> {
  private final Match match;
  private final FeatureReference<ProjectileDefinition> projectileReference;
  private Vector origin;
  private final Vector destination;
  private final Filter damageFilter;

  public LaunchProjectileAction(
      Class<T> scope,
      Match match,
      FeatureReference<ProjectileDefinition> projectileReference,
      Vector origin,
      Vector destination,
      @Nullable Filter damageFilter) {
    super(scope);
    this.match = match;
    this.projectileReference = projectileReference;
    this.origin = origin;
    this.destination = destination;
    this.damageFilter = damageFilter;
  }

  public Vector getOrigin() {
    return origin;
  }

  public Vector getDestination() {
    return destination;
  }

  public @Nullable Filter getDamageFilter() {
    return damageFilter;
  }

  @Override
  public void trigger (T t) {
    ProjectileDefinition projectile = projectileReference.get();
    Vector direction = destination.clone().subtract(origin.normalize());
//    double velocity = direction.multiply(projectile.velocity);
    Location loc = new Location(match.getWorld(), 0, 0, 0);
//    boolean realProjectile = Projectile.class.isAssignableFrom(projectile.projectile);
//
//
//    Entity entity
//    if (realProjectile) {
//      entity = spawn
//    }
//    Entity entity = loc.getWorld().spawnEntity(loc, projectile.getEntityType())
  }
}