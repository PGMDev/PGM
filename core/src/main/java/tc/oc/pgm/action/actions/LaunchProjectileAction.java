package tc.oc.pgm.action.actions;

import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.util.Vector;
import org.jspecify.annotations.Nullable;
import tc.oc.pgm.api.PGM;
import tc.oc.pgm.api.feature.FeatureReference;
import tc.oc.pgm.api.filter.Filter;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.filters.Filterable;
import tc.oc.pgm.projectile.ProjectileDefinition;

public class LaunchProjectileAction<T extends Filterable<?>> extends AbstractAction<T> {
  private final FeatureReference<ProjectileDefinition> projectileReference;
  private final Vector origin;
  private final @Nullable Vector toward;
  private final float yaw;
  private final float pitch;
  private final Filter damageFilter;

  public LaunchProjectileAction(
      Class<T> scope,
      FeatureReference<ProjectileDefinition> projectileReference,
      Vector origin,
      @Nullable Vector toward,
      float yaw,
      float pitch,
      @Nullable Filter damageFilter) {
    super(scope);
    this.projectileReference = projectileReference;
    this.origin = origin;
    this.toward = toward;
    this.yaw = yaw;
    this.pitch = pitch;
    this.damageFilter = damageFilter;
  }

  public @Nullable Filter getDamageFilter() {
    return damageFilter;
  }

  @Override
  public void trigger(T t) {
    Match match = t.getMatch();
    ProjectileDefinition def = projectileReference.get();

    Vector dir;
    if (toward != null) {
      dir = toward.clone().subtract(origin).normalize();
    } else {
      Location dirLoc = new Location(null, 0, 0, 0, yaw, pitch);
      dir = dirLoc.getDirection();
    }
    dir.multiply(def.getVelocity());
    Location loc = origin.toLocation(match.getWorld());

    Entity proj = match.getWorld().spawn(loc, def.getProjectile());
    proj.setVelocity(dir);
    proj.setMetadata("projectileDefinition", new FixedMetadataValue(PGM.get(), def));
    proj.setMetadata("launchProjectile", new FixedMetadataValue(PGM.get(), this));
  }
}
