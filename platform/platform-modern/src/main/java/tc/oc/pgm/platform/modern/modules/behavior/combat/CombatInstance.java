package tc.oc.pgm.platform.modern.modules.behavior.combat;

import io.papermc.paper.entity.LookAnchor;
import java.time.Duration;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.pathfinder.Path;
import org.bukkit.attribute.Attribute;
import org.bukkit.craftbukkit.CraftWorld;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.api.time.Tick;
import tc.oc.pgm.platform.modern.modules.mannequin.Mannequin;

public class CombatInstance {

  public static final Float DEFAULT_ATTACK_RANGE = 3.0f;
  public static final Duration DEFAULT_ATTACK_INTERVAL = Duration.ofSeconds(1);
  public static final Duration DEFAULT_PANIC_DURATION = Duration.ofSeconds(5);

  private final Mannequin mannequin;
  private final CombatBehavior behavior;
  private @Nullable MatchPlayer target;
  private long aggroExpiryTick = Long.MAX_VALUE;
  private long nextAttackTick = 0;
  private final double rangeSq;
  private final long intervalTicks;
  private final net.minecraft.world.entity.monster.zombie.Zombie ghost;
  private final double straySq;
  private final long returnAfterTicks;
  private long idleSinceTick = -1;
  private @Nullable Path currentPath;
  long nextRepathTick;
  private long nextWanderTick = 0;
  private Vector lastWanderProgressPos = null;
  private long lastWanderProgressTick = 0;
  private final long panicDurationTicks;
  private @Nullable MatchPlayer panicSource;
  private long panicExpiryTick = 0;

  public CombatInstance(Mannequin mannequin, CombatBehavior behavior) {
    this.mannequin = mannequin;
    this.behavior = behavior;
    this.rangeSq = behavior.getRange() * behavior.getRange();
    this.intervalTicks = behavior.getInterval().toMillis() / 50;
    this.ghost = new net.minecraft.world.entity.monster.zombie.Zombie(
        EntityType.ZOMBIE, ((CraftWorld) mannequin.getEntity().getWorld()).getHandle());
    this.straySq =
        behavior.getStrayDis() != null ? behavior.getStrayDis() * behavior.getStrayDis() : -1;
    this.returnAfterTicks =
        behavior.getReturnAfter() != null ? behavior.getReturnAfter().toMillis() / 50 : 0;
    this.panicDurationTicks =
        (behavior.getPanicDuration() != null ? behavior.getPanicDuration() : DEFAULT_PANIC_DURATION)
                .toMillis()
            / 50;
  }

  public boolean matches(org.bukkit.entity.Entity entity) {
    return mannequin.getEntityId().equals(entity.getUniqueId());
  }

  public void onAttacked(MatchPlayer attacker, Tick now) {
    if (behavior.getHostility() == HostilityType.PASSIVE) {
      if (behavior.isPanic()) {
        panicSource = attacker;
        panicExpiryTick = now.tick + panicDurationTicks;
      }
      return;
    }

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
        mannequin.getEntity().lookAt(bukkit.getLocation(), LookAnchor.EYES);
        stepAlongPath();
      }
    }

    if (target == null && behavior.getReturnHome() && behavior.getHome() != null) {
      boolean atHome;
      if (straySq >= 0) {
        var center = behavior.getHome().getStatic(match).getBounds().getCenterPoint();
        var loc = mannequin.getLocation();
        double dx = center.getX() - loc.getX();
        double dz = center.getZ() - loc.getZ();
        // Ignore dy to simplify math and to avoid non-full blocks/vertical jittering
        atHome = dx * dx + dz * dz <= straySq;
      } else {
        atHome = behavior.getHome().contains(mannequin.getEntity());
      }

      if (atHome) {
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
                    new BlockPos((int) center.getX(), (int) center.getY(), (int) center.getZ()), 0);
            nextRepathTick = now.tick + 10;
          }
          stepAlongPath();
        }
      }
    } else if (target != null) {
      idleSinceTick = -1;
    }

    if (target == null && behavior.isWander() && atHomeCanWander(match)) {
      if (currentPath == null || currentPath.isDone()) {
        if (now.tick >= nextWanderTick) {
          Vector wanderPoint = pickWanderPoint(match);

          if (wanderPoint != null) {
            var loc = mannequin.getLocation();
            ghost.setPos(loc.getX(), loc.getY(), loc.getZ());
            ghost.setOnGround(true);
            currentPath = ghost
                .getNavigation()
                .createPath(
                    new BlockPos(
                        wanderPoint.getBlockX(), wanderPoint.getBlockY(), wanderPoint.getBlockZ()),
                    0);
            lastWanderProgressPos = loc.toVector();
            lastWanderProgressTick = now.tick;
          }
          nextWanderTick = now.tick + randomIdleTicks(match);
        }
      } else {
        var pos = mannequin.getLocation().toVector();

        if (lastWanderProgressPos == null || pos.distanceSquared(lastWanderProgressPos) > 0.25) {
          lastWanderProgressPos = pos;
          lastWanderProgressTick = now.tick;
        } else if (now.tick - lastWanderProgressTick > 60) {
          currentPath = null;
          nextWanderTick = now.tick + randomIdleTicks(match);
          return;
        }
        stepAlongPath();
      }
    }

    if (behavior.getHostility() == HostilityType.PASSIVE
        && behavior.isPanic()
        && target == null
        && now.tick < panicExpiryTick) {
      if (panicSource != null) {
        Vector panicPoint = pickPanicPoint(panicSource, match);

        if (panicPoint != null && now.tick >= nextRepathTick) {
          var loc = mannequin.getLocation();
          ghost.setPos(loc.getX(), loc.getY(), loc.getZ());
          ghost.setOnGround(true);

          currentPath = ghost
              .getNavigation()
              .createPath(
                  new BlockPos(
                      panicPoint.getBlockX(), panicPoint.getBlockY(), panicPoint.getBlockZ()),
                  0);
          nextRepathTick = now.tick + 20;
        }

        if (currentPath != null && !currentPath.isDone()) {
          var nodePos = currentPath.getNextNodePos();
          var loc = mannequin.getLocation();
          var dir = new Vector(
              nodePos.getX() + 0.5 - loc.getX(),
              0,
              nodePos.getZ() + 0.5 - loc.getZ()); // Skip y axis for now

          if (dir.lengthSquared() < 0.25) {
            currentPath.advance();
          } else {
            mannequin
                .getEntity()
                .setVelocity(dir.normalize().multiply(0.28)); // 0.28 bps aka sprint speed
          }
        }
      }
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

  public boolean hasTarget() {
    return target != null;
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

  private void stepAlongPath() {
    if (currentPath == null || currentPath.isDone()) return;
    var nodePos = currentPath.getNextNodePos();
    var loc = mannequin.getLocation();
    var direction = new org.bukkit.util.Vector(
        nodePos.getX() + 0.5 - loc.getX(), 0, nodePos.getZ() + 0.5 - loc.getZ());

    if (direction.lengthSquared() < 0.25) {
      currentPath.advance();
      return;
    }
    mannequin.getEntity().setVelocity(direction.normalize().multiply(0.15));
  }

  private @Nullable Vector pickWanderPoint(Match match) {
    var home = behavior.getHome().getStatic(match);
    var bounds = home.getBounds();
    var min = bounds.getMin();
    var max = bounds.getMax();
    var random = match.getRandom();

    Vector wanderTarget = null;
    for (int i = 0; i < 4; i++) {
      double x = min.getX() + random.nextDouble() * (max.getX() - min.getX());
      double z = min.getZ() + random.nextDouble() * (max.getZ() - min.getZ());
      Vector candidate = new Vector(x, mannequin.getLocation().getY(), z);
      if (home.contains(candidate)) {
        wanderTarget = candidate;
        break;
      }
    }

    return wanderTarget;
  }

  private long randomIdleTicks(Match match) {
    return 80 + match.getRandom().nextInt(120);
  }

  private boolean atHomeCanWander(Match match) {
    if (behavior.getHome() == null) return true;
    return behavior.getHome().contains(mannequin.getEntity().getLocation());
  }

  private @Nullable Vector pickPanicPoint(MatchPlayer attacker, Match match) {
    var attackerLoc = attacker.getBukkit().getLocation();
    var manLoc = mannequin.getLocation();
    var away = manLoc.toVector().subtract(attackerLoc.toVector());

    if (away.lengthSquared() < 0.01) {
      away = new Vector(1, 0, 0);
      away.setY(0).normalize();
    }

    double dis = 5 + match.getRandom().nextDouble() * 4;
    double jit = (match.getRandom().nextDouble() - 0.5) * 4;
    return manLoc
        .toVector()
        .add(away.multiply(dis))
        .add(new Vector(-away.getZ() * jit, 0, away.getX() * jit));
  }
}
