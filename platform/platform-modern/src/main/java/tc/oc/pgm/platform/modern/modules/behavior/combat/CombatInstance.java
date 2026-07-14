package tc.oc.pgm.platform.modern.modules.behavior.combat;

import org.bukkit.entity.Player;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.api.time.Tick;
import tc.oc.pgm.platform.modern.modules.mannequin.Mannequin;

import javax.annotation.Nullable;

public class CombatInstance {

  private static final double ATTACK_RANGE_SQ = 3.0 * 3.0;
  private static final long ATTACK_INTERVAL_TICKS = 20;

  private final Mannequin mannequin;
  private final CombatBehavior behavior;
  private  @Nullable MatchPlayer target;
  private long aggroExpiryTick = Long.MAX_VALUE;
  private long nextAttackTick = 0;

  public CombatInstance(Mannequin mannequin, CombatBehavior behavior) {
    this.mannequin = mannequin;
    this.behavior = behavior;
  }

  public boolean matches(org.bukkit.entity.Entity entity) {
    return mannequin.getEntityId().equals(entity.getUniqueId());
  }

  public void onAttacked(MatchPlayer attacker, Tick now) {
    if (behavior.getHostility() == HostilityType.PASSIVE) return;
    if (target == null || target == attacker) {
      target = attacker;
      refreshExpiry(now);
    }
  }

  public void tick(Match match, Tick now) {
    validateTarget(now);

    if (target == null && behavior.getHostility() == HostilityType.HOSTILE) {
      acquireTarget(match, now);
    }

    if (target != null && now.tick >= nextAttackTick && inAttackRange()) {
      mannequin.getEntity().attack(target.getBukkit());
      nextAttackTick = now.tick + ATTACK_INTERVAL_TICKS;
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
    return behavior.getHome() != null
        && !behavior.getHome().contains(bukkit.getLocation());
  }

  private boolean inAttackRange() {
    Player bukkit = target.getBukkit();
    return bukkit != null
        && bukkit.getWorld().equals(mannequin.getEntity().getWorld())
        && bukkit.getLocation().distanceSquared(mannequin.getEntity().getLocation())
        <= ATTACK_RANGE_SQ;
  }
}
