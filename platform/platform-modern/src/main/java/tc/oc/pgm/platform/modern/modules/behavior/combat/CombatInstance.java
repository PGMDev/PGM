package tc.oc.pgm.platform.modern.modules.behavior.combat;

import io.papermc.paper.entity.LookAnchor;
import java.time.Duration;
import java.util.Map;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.level.pathfinder.PathType;
import org.bukkit.attribute.Attribute;
import org.bukkit.craftbukkit.CraftWorld;
import org.bukkit.entity.Player;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.api.time.Tick;
import tc.oc.pgm.platform.modern.modules.behavior.PathWalking;
import tc.oc.pgm.platform.modern.modules.mannequin.Mannequin;

public class CombatInstance {

  public static final Float DEFAULT_ATTACK_RANGE = 3.0f;
  public static final Duration DEFAULT_ATTACK_INTERVAL = Duration.ofSeconds(1);

  private final Mannequin mannequin;
  private final CombatBehavior behavior;
  private @Nullable MatchPlayer target;
  private long aggroExpiryTick = -1;
  private long nextAttackTick;
  private final double rangeSq;
  private final long intervalTicks;
  private final net.minecraft.world.entity.monster.zombie.Zombie ghost;
  private final double straySq;
  private final long returnAfterTicks;
  private long idleSinceTick = -1;
  private @Nullable Path currentPath;
  private long nextRepathTick;
  private final @Nullable PanicInstance panic;
  private final @Nullable WanderInstance wander;
  private static final Map<PathType, Float> DANGER_PENALTIES = Map.of(
      PathType.DAMAGE_OTHER, -1.0F,
      PathType.DANGER_OTHER, -1.0F,
      PathType.DAMAGE_FIRE, -1.0F,
      PathType.DANGER_FIRE, -1.0F,
      PathType.DAMAGE_CAUTIOUS, -1.0F,
      PathType.LAVA, -1.0F);

  public CombatInstance(Mannequin mannequin, CombatBehavior behavior, boolean avoidDanger) {
    this.mannequin = mannequin;
    this.behavior = behavior;
    this.rangeSq = behavior.getRange() * behavior.getRange();
    this.intervalTicks = behavior.getInterval().toMillis() / 50;
    this.ghost = new net.minecraft.world.entity.monster.zombie.Zombie(
        EntityType.ZOMBIE, ((CraftWorld) mannequin.getEntity().getWorld()).getHandle());

    if (avoidDanger) {
      DANGER_PENALTIES.forEach(this.ghost::setPathfindingMalus);
    }

    this.straySq =
        behavior.getStrayDis() != null ? behavior.getStrayDis() * behavior.getStrayDis() : -1;
    this.returnAfterTicks =
        behavior.getReturnAfter() != null ? behavior.getReturnAfter().toMillis() / 50 : 0;
    this.panic = behavior.isPanic() ? new PanicInstance(mannequin, behavior, ghost) : null;
    this.wander = behavior.isWander() ? new WanderInstance(mannequin, behavior, ghost) : null;
  }

  public boolean matches(org.bukkit.entity.Entity entity) {
    return mannequin.getEntityId().equals(entity.getUniqueId());
  }

  public void onAttacked(MatchPlayer attacker, Tick now) {
    if (behavior.getHostility() == HostilityType.PASSIVE) {
      if (panic != null) {
        panic.startPanic(attacker, now);
      }

      if (wander != null) {
        wander.clearWanderPath();
      }
      return;
    }

    if (target == null || target == attacker) {
      target = attacker;
      refreshExpiry(now);
      if (wander != null) {
        wander.clearWanderPath();
      }
    }
  }

  public void tick(Match match, Tick now) {
    validateTarget(now);
    if (panic != null) {
      panic.tick(match, now);
    }

    if (target == null && behavior.getHostility() == HostilityType.HOSTILE) {
      acquireTarget(match, now);
    }

    if (target != null && now.tick >= nextAttackTick && inAttackRange(target)) {
      mannequin.getEntity().swingMainHand();
      target
          .getBukkit()
          .damage(
              mannequin.getEntity().getAttribute(Attribute.ATTACK_DAMAGE).getValue(),
              mannequin.getEntity());
      nextAttackTick = now.tick + intervalTicks;
    }

    if (target != null && !inAttackRange(target)) {
      Player bukkit = target.getBukkit();
      if (bukkit != null) {
        if (now.tick >= nextRepathTick) {
          var loc = mannequin.getLocation();
          ghost.setPos(loc.getX(), loc.getY(), loc.getZ());
          ghost.setOnGround(true);
          currentPath = ghost
              .getNavigation()
              .createPath(
                  new BlockPos(
                      bukkit.getLocation().getBlockX(),
                      bukkit.getLocation().getBlockY(),
                      bukkit.getLocation().getBlockZ()),
                  0);
          nextRepathTick = now.tick + 10;
        }

        PathWalking.step(mannequin, currentPath, 0.15);
        mannequin.getEntity().lookAt(bukkit.getLocation(), LookAnchor.EYES);
      }
    }

    if (target == null
        && behavior.getReturnHome()
        && behavior.getHome() != null
        && (panic == null || !panic.isPanicking(now))) {
      if (atHome(match)) {
        idleSinceTick = -1;
        currentPath = null;
      } else {
        if (idleSinceTick == -1) idleSinceTick = now.tick;
        if (now.tick - idleSinceTick >= returnAfterTicks) {
          if (now.tick >= nextRepathTick) {
            var center = behavior.getHome().getStatic(match).getBounds().getCenterPoint();
            var loc = mannequin.getLocation();
            ghost.setPos(loc.getX(), loc.getY(), loc.getZ());
            ghost.setOnGround(true);
            currentPath = ghost
                .getNavigation()
                .createPath(
                    new BlockPos(
                        (int) Math.floor(center.getX()), (int) Math.floor(center.getY()), (int)
                            Math.floor(center.getZ())),
                    0);
            nextRepathTick = now.tick + 10;
          }
          PathWalking.step(mannequin, currentPath, 0.15);
        }
      }
    } else if (target != null) {
      idleSinceTick = -1;
    }

    if (target == null
        && behavior.isWander()
        && atHome(match)
        && (panic == null || !panic.isPanicking(now))) {
      wander.tick(match, now);
    }
  }

  private void validateTarget(Tick now) {
    if (target == null) return;
    Player bukkit = target.getBukkit();
    if (bukkit == null
        || target.isDead()
        || !target.isParticipating()
        || aggroExpiryTick != -1 && now.tick > aggroExpiryTick
        || outsideHome(bukkit)) {
      target = null;
      aggroExpiryTick = -1;
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

  public boolean hasTarget() {
    return target != null;
  }

  private void refreshExpiry(Tick now) {
    aggroExpiryTick =
        behavior.getDuration() != null ? now.tick + behavior.getDuration().toMillis() / 50 : -1;
  }

  private boolean atHome(Match match) {
    if (behavior.getHome() == null) return true;
    if (straySq >= 0) {
      var center = behavior.getHome().getStatic(match).getBounds().getCenterPoint();
      var loc = mannequin.getLocation();
      double dx = center.getX() - loc.getX();
      double dz = center.getZ() - loc.getZ();
      return dx * dx + dz * dz <= straySq;
    }
    return behavior.getHome().contains(mannequin.getEntity());
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

  public boolean isPanicking(Tick now) {
    return panic != null && panic.isPanicking(now);
  }

  public void onEnvironmentalDamage(Tick now) {
    if (behavior.getHostility() == HostilityType.PASSIVE && panic != null) {
      panic.startPanic(null, now);
    }
  }
}
