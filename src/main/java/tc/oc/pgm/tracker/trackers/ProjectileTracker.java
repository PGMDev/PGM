package tc.oc.pgm.tracker.trackers;

import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.projectiles.ProjectileSource;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.projectile.EntityLaunchEvent;
import tc.oc.pgm.projectile.ProjectileDefinition;
import tc.oc.pgm.projectile.ProjectileMatchModule;
import tc.oc.pgm.tracker.TrackerMatchModule;
import tc.oc.pgm.tracker.damage.PhysicalInfo;
import tc.oc.pgm.tracker.damage.ProjectileInfo;

/**
 * Updates the state of launched projectiles with info about the shooter. Uses {@link Entity}
 * instead of {@link org.bukkit.entity.Projectile} to support custom projectiles
 */
public class ProjectileTracker extends AbstractTracker<ProjectileInfo> {

  public ProjectileTracker(TrackerMatchModule tmm, Match match) {
    super(ProjectileInfo.class, tmm, match);
  }

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onProjectileLaunch(ProjectileLaunchEvent event) {
    handleLaunch(event.getEntity(), event.getEntity().getShooter());
  }

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onCustomProjectileLaunch(EntityLaunchEvent event) {
    // OwnedMobTracker handles mob launches
    if (!(event.getEntity() instanceof LivingEntity)) {
      handleLaunch(event.getEntity(), event.getSource());
    }
  }

  void handleLaunch(Entity projectile, ProjectileSource source) {
    PhysicalInfo projectileInfo = entities().resolveEntity(projectile);
    if (!(projectileInfo instanceof ProjectileInfo)) {
      ProjectileDefinition definition = ProjectileMatchModule.getProjectileDefinition(projectile);
      String customName = definition == null ? null : definition.getName();
      entities()
          .trackEntity(
              projectile,
              new ProjectileInfo(
                  projectileInfo,
                  tmm.resolveShooter(source),
                  projectile.getLocation(),
                  customName));
    }
  }
}
