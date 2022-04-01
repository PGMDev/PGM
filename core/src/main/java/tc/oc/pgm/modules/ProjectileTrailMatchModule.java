package tc.oc.pgm.modules;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import org.bukkit.Color;
import org.bukkit.Particle;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.projectiles.ProjectileSource;
import tc.oc.pgm.api.PGM;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.match.MatchModule;
import tc.oc.pgm.api.match.MatchScope;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.api.setting.SettingKey;
import tc.oc.pgm.api.setting.SettingValue;
import tc.oc.pgm.events.ListenerScope;
import tc.oc.pgm.util.bukkit.MetadataUtils;

@ListenerScope(MatchScope.RUNNING)
public class ProjectileTrailMatchModule implements MatchModule, Listener {

  private static final String TRAIL_META = "projectile_trail_color";
  private static final String CRITICAL_META = "arrow_is_critical";

  private final Match match;

  public ProjectileTrailMatchModule(Match match) {
    this.match = match;
    match
        .getExecutor(MatchScope.RUNNING)
        .scheduleAtFixedRate(this::checkMatchProjectiles, 0l, 50, TimeUnit.MILLISECONDS);
  }

  public void checkMatchProjectiles() {
    match.getWorld().getEntitiesByClass(Projectile.class).stream()
        .filter(projectile -> projectile.hasMetadata(TRAIL_META))
        .forEach(
            projectile -> {
              if (projectile.isDead() || projectile.isOnGround()) {
                projectile.removeMetadata(TRAIL_META, PGM.get());
              } else {
                Color color =
                    projectile.hasMetadata(TRAIL_META)
                        ? (Color)
                            MetadataUtils.getMetadata(projectile, TRAIL_META, PGM.get()).value()
                        : null;

                final Collection<MatchPlayer> matchPlayers = match.getPlayers();

                // ParticleBuilder.spawn is thread safe, is everything else?
                PGM.get()
                    .getAsyncExecutor()
                    .execute(
                        () -> {
                          Set<MatchPlayer> playerSet = new HashSet<>(matchPlayers);
                          Set<MatchPlayer> filtered =
                              playerSet.stream()
                                  .filter(
                                      matchPlayer ->
                                          matchPlayer
                                              .getSettings()
                                              .getValue(SettingKey.EFFECTS)
                                              .equals(SettingValue.EFFECTS_ON))
                                  .collect(Collectors.toSet());
                          playerSet.removeAll(filtered);

                          Set<Player> colored =
                              filtered.stream()
                                  .map(MatchPlayer::getBukkit)
                                  .collect(Collectors.toSet());
                          Set<Player> crit =
                              playerSet.stream()
                                  .map(MatchPlayer::getBukkit)
                                  .collect(Collectors.toSet());

                          Particle.REDSTONE
                              .builder()
                              .color(color)
                              .location(projectile.getLocation())
                              .receivers(colored)
                              .spawn();

                          Particle.CRIT
                              .builder()
                              .location(projectile.getLocation())
                              .receivers(crit)
                              .spawn();
                        });
              }
            });
  }

  private float rgbToParticle(int rgb) {
    return Math.max(0.001f, (rgb / 255.0f));
  }

  private boolean isCriticalArrow(Projectile projectile) {
    if (projectile instanceof Arrow) {
      final Arrow arrow = (Arrow) projectile;
      if (arrow.hasMetadata(CRITICAL_META)) {
        return MetadataUtils.getMetadata(projectile, CRITICAL_META, PGM.get()).asBoolean();
      }
    }
    return false;
  }

  static Player getShooter(Projectile projectile) {
    ProjectileSource shooter = projectile.getShooter();
    return shooter instanceof Player ? (Player) shooter : null;
  }

  @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
  public void onProjectileLaunch(ProjectileLaunchEvent event) {
    MatchPlayer player = match.getPlayer(getShooter(event.getEntity()));
    if (player != null) {
      final Projectile projectile = event.getEntity();
      projectile.setMetadata(
          TRAIL_META, new FixedMetadataValue(PGM.get(), player.getParty().getFullColor()));
      // Set critical metadata to false in order to remove default particle trail.
      // The metadata will be restored just before the arrow hits something.
      if (projectile instanceof Arrow) {
        final Arrow arrow = (Arrow) projectile;
        arrow.setMetadata(CRITICAL_META, new FixedMetadataValue(PGM.get(), arrow.isCritical()));
        arrow.setCritical(false);
      }
    }
  }

  @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
  public void onProjectileHit(ProjectileHitEvent event) {
    MatchPlayer player = match.getPlayer(getShooter(event.getEntity()));
    if (player != null) {
      final Projectile projectile = event.getEntity();
      projectile.removeMetadata(TRAIL_META, PGM.get());
      // Restore critical metadata to arrows if applicable
      if (projectile instanceof Arrow) {
        final Arrow arrow = (Arrow) projectile;
        if (arrow.hasMetadata(CRITICAL_META)) {
          arrow.setCritical(MetadataUtils.getMetadata(arrow, CRITICAL_META, PGM.get()).asBoolean());
        }
      }
    }
  }
}
