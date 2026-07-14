package tc.oc.pgm.platform.modern.modules.behavior.combat;

import java.time.Duration;
import javax.annotation.Nullable;
import org.bukkit.entity.Player;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.api.time.Tick;
import tc.oc.pgm.platform.modern.modules.mannequin.Mannequin;

public class CombatInstance {

  public static final Float DEFAULT_ATTACK_RANGE = 3.0f;
  public static final Duration DEFAULT_ATTACK_INTERVAL = Duration.ofSeconds(1);

  private final Mannequin mannequin;
  private final CombatBehavior behavior;
  private @Nullable MatchPlayer target;
  private long aggroExpiryTick = Long.MAX_VALUE;
  private long nextAttackTick = 0;
  private final double rangeSq;
  private final long intervalTicks;

  public CombatInstance(Mannequin mannequin, CombatBehavior behavior) {
    this.mannequin = mannequin;
    this.behavior = behavior;
    this.rangeSq = behavior.getRange() * behavior.getRange();
    this.intervalTicks = behavior.getInterval().toMillis() / 50;
  }

  public boolean matches(org.bukkit.entity.Entity entity) {
    return mannequin.getEntityId().equals(entity.getUniqueId());
  }

  public void onAttacked(MatchPlayer attacker, Tick now) {
    org.bukkit.Bukkit.getLogger()
        .info("[behavior-debug] onAttacked, hostility=" + behavior.getHostility()
            + " target-before=" + target); // REMOVE LATER
    if (behavior.getHostility() == HostilityType.PASSIVE) return;
    if (target == null || target == attacker) {
      target = attacker;
      refreshExpiry(now);
    }
  }

  public void tick(Match match, Tick now) {
    validateTarget(now);
    if (target != null)
      org.bukkit.Bukkit.getLogger()
          .info("[behavior-debug] ticking with target=" + target.getNameLegacy() + " inRange="
              + inAttackRange(target)); // REMOVE LATER

    if (target == null && behavior.getHostility() == HostilityType.HOSTILE) {
      acquireTarget(match, now);
    }

    if (target != null && now.tick >= nextAttackTick && inAttackRange(target)) {
      org.bukkit.Bukkit.getLogger().info("[behavior-debug] ATTACKING"); // REMOVE LATER
      mannequin.getEntity().swingMainHand();
      target.getBukkit().damage(2.0, mannequin.getEntity());
      nextAttackTick = now.tick + intervalTicks;
    }
  }

  private void validateTarget(Tick now) {
    if (target == null) return;
    Player bukkit = target.getBukkit();
    if (bukkit == null
        || target.isDead()
        || !target.isParticipating()
        || now.tick > aggroExpiryTick
        || outsideHome(bukkit)) {
      target = null;
      aggroExpiryTick = Long.MAX_VALUE;
    }
  }

  private void acquireTarget(Match match, Tick now) {
    if (behavior.getHome() == null) return;
    for (MatchPlayer player : match.getParticipants()) {
      Player bukkit = player.getBukkit();
      if (bukkit == null || player.isDead()) continue;
      if (behavior.getHome().contains(bukkit.getLocation())) {
        target = player;
        refreshExpiry(now);
        return;
      }
    }
  }

  private void refreshExpiry(Tick now) {
    aggroExpiryTick = behavior.getDuration() != null
        ? now.tick + behavior.getDuration().toMillis() / 50
        : Long.MAX_VALUE;
  }

  private boolean outsideHome(Player bukkit) {
    return behavior.getHome() != null && !behavior.getHome().contains(bukkit.getLocation());
  }

  private boolean inAttackRange(MatchPlayer target) {
    Player bukkit = target.getBukkit();
    if (bukkit == null) return false;
    return bukkit.getWorld().equals(mannequin.getEntity().getWorld())
        && bukkit.getLocation().distanceSquared(mannequin.getEntity().getLocation()) <= rangeSq;
  }
}
