package tc.oc.pgm.modules;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import org.bukkit.Color;
import org.bukkit.Effect;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.metadata.MetadataValue;
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

  private static final String TRAIL_KEY = "projectile_trail_color";
  private static final String CRITICAL_KEY = "arrow_is_critical";

  private final Match match;
  private final MetadataValue criticalValue = MetadataUtils.createMetadataValue(PGM.get(), true);
  private final Map<Color, MetadataValue> trailValues = new HashMap<>();

  public ProjectileTrailMatchModule(Match match) {
    this.match = match;
    match
        .getExecutor(MatchScope.RUNNING)
        .scheduleAtFixedRate(this::checkMatchProjectiles, 0L, 50, TimeUnit.MILLISECONDS);
  }

  public void checkMatchProjectiles() {
    Collection<Projectile> projectiles = match.getWorld().getEntitiesByClass(Projectile.class);
    if (projectiles.isEmpty()) return;

    // Pre-compute the list of players to show and hide arrow trails from. This avoids each arrow
    // re-calculating settings
    List<MatchPlayer> particleViewers = new ArrayList<>(match.getPlayers().size());
    List<MatchPlayer> noParticleViewers = new ArrayList<>(match.getPlayers().size());
    for (MatchPlayer player : match.getPlayers()) {
      (player.getSettings().getValue(SettingKey.EFFECTS).equals(SettingValue.EFFECTS_ON)
              ? particleViewers
              : noParticleViewers)
          .add(player);
    }

    for (Projectile projectile : projectiles) {
      if (!projectile.hasMetadata(TRAIL_KEY)) continue;
      if (projectile.isDead() || projectile.isOnGround()) {
        projectile.removeMetadata(TRAIL_KEY, PGM.get());
        return;
      }
      Color color = MetadataUtils.getMetadataValue(projectile, TRAIL_KEY, PGM.get());
      for (MatchPlayer player : particleViewers) {
        player
            .getBukkit()
            .spigot()
            .playEffect(
                projectile.getLocation(),
                Effect.COLOURED_DUST,
                0,
                0,
                rgbToParticle(color.getRed()),
                rgbToParticle(color.getGreen()),
                rgbToParticle(color.getBlue()),
                1,
                0,
                50);
      }

      // Play the critical effect to those who have effects off, to replicate original
      // arrow behavior
      if (!projectile.hasMetadata(CRITICAL_KEY)) continue;
      for (MatchPlayer player : noParticleViewers) {
        player
            .getBukkit()
            .spigot()
            .playEffect(projectile.getLocation(), Effect.CRIT, 0, 0, 0, 0, 0, 1, 0, 50);
      }
    }
  }

  private float rgbToParticle(int rgb) {
    return Math.max(0.001f, (rgb / 255.0f));
  }

  static Player getShooter(Projectile projectile) {
    ProjectileSource shooter = projectile.getShooter();
    return shooter instanceof Player ? (Player) shooter : null;
  }

  @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
  public void onProjectileLaunch(ProjectileLaunchEvent event) {
    MatchPlayer player = match.getPlayer(getShooter(event.getEntity()));
    if (player == null) return;
    final Projectile projectile = event.getEntity();
    projectile.setMetadata(TRAIL_KEY, getOrCreateColorValue(player.getParty().getFullColor()));
    // Set critical metadata to false in order to remove default particle trail.
    // The metadata will be restored just before the arrow hits something.
    if (projectile instanceof Arrow && ((Arrow) projectile).isCritical()) {
      projectile.setMetadata(CRITICAL_KEY, criticalValue);
      ((Arrow) projectile).setCritical(false);
    }
  }

  @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
  public void onProjectileHit(ProjectileHitEvent event) {
    MatchPlayer player = match.getPlayer(getShooter(event.getEntity()));
    if (player == null) return;
    final Projectile projectile = event.getEntity();
    projectile.removeMetadata(TRAIL_KEY, PGM.get());
    // Restore critical metadata to arrows if applicable
    if (projectile instanceof Arrow && projectile.hasMetadata(CRITICAL_KEY)) {
      ((Arrow) projectile).setCritical(true);
    }
  }

  private MetadataValue getOrCreateColorValue(Color color) {
    return trailValues.computeIfAbsent(color, c -> MetadataUtils.createMetadataValue(PGM.get(), c));
  }
}
