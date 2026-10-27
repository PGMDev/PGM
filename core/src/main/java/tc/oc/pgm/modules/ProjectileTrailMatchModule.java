package tc.oc.pgm.modules;

import static tc.oc.pgm.util.bukkit.Effects.EFFECTS;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import org.bukkit.Color;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.projectiles.ProjectileSource;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.match.MatchModule;
import tc.oc.pgm.api.match.MatchScope;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.api.setting.SettingKey;
import tc.oc.pgm.api.setting.SettingValue;
import tc.oc.pgm.events.ListenerScope;

@ListenerScope(MatchScope.RUNNING)
public class ProjectileTrailMatchModule implements MatchModule, Listener {

  private final Match match;
  private final Map<Projectile, Trail> trails = new HashMap<>();

  public ProjectileTrailMatchModule(Match match) {
    this.match = match;
    match
        .getExecutor(MatchScope.RUNNING)
        .scheduleAtFixedRate(this::checkMatchProjectiles, 0L, 50, TimeUnit.MILLISECONDS);
  }

  public void checkMatchProjectiles() {
    trails.keySet().removeIf(projectile -> !projectile.isValid() || projectile.isOnGround());
    if (trails.isEmpty()) return;

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

    trails.forEach((projectile, trail) -> {
      for (MatchPlayer player : particleViewers) {
        EFFECTS.coloredDust(player.getBukkit(), projectile.getLocation(), trail.color());
      }

      // Play the critical effect to those who have effects off, to replicate original
      // arrow behavior
      if (!trail.critical()) return;
      for (MatchPlayer player : noParticleViewers) {
        EFFECTS.criticalArrow(player.getBukkit(), projectile.getLocation());
      }
    });
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
    // Set critical to false in order to remove default particle trail.
    // It will be restored just before the arrow hits something.
    boolean critical = projectile instanceof Arrow arrow && arrow.isCritical();
    if (critical) ((Arrow) projectile).setCritical(false);
    trails.put(projectile, new Trail(player.getParty().getFullColor(), critical));
  }

  @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
  public void onProjectileHit(ProjectileHitEvent event) {
    MatchPlayer player = match.getPlayer(getShooter(event.getEntity()));
    if (player == null) return;
    final Projectile projectile = event.getEntity();
    Trail trail = trails.remove(projectile);
    // Restore critical to arrows if applicable
    if (trail != null && trail.critical()) {
      ((Arrow) projectile).setCritical(true);
    }
  }

  private record Trail(Color color, boolean critical) {}
}
