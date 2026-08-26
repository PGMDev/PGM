package tc.oc.pgm.platform.modern.modules.behavior.evade;

import io.papermc.paper.entity.LookAnchor;
import java.time.Duration;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.level.pathfinder.Path;
import org.bukkit.entity.Entity;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.util.Vector;
import org.jspecify.annotations.Nullable;
import tc.oc.pgm.api.filter.Filter;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.api.time.Tick;
import tc.oc.pgm.platform.modern.modules.behavior.PathWalking;
import tc.oc.pgm.platform.modern.modules.mannequin.Mannequin;

public class EvadeInstance {

  public static final Duration DEFAULT_PANIC_DURATION = Duration.ofSeconds(5);
  private static final Set<EntityDamageEvent.DamageCause> PANIC_CAUSES = Set.of(
      EntityDamageEvent.DamageCause.CONTACT,
      EntityDamageEvent.DamageCause.FALL,
      EntityDamageEvent.DamageCause.FIRE,
      EntityDamageEvent.DamageCause.FIRE_TICK,
      EntityDamageEvent.DamageCause.LAVA,
      EntityDamageEvent.DamageCause.DROWNING,
      EntityDamageEvent.DamageCause.BLOCK_EXPLOSION,
      EntityDamageEvent.DamageCause.ENTITY_EXPLOSION,
      EntityDamageEvent.DamageCause.LIGHTNING,
      EntityDamageEvent.DamageCause.FALLING_BLOCK,
      EntityDamageEvent.DamageCause.HOT_FLOOR,
      EntityDamageEvent.DamageCause.CAMPFIRE);

  private final Mannequin mannequin;
  private final EvadeBehavior behavior;
  private final Zombie ghost;

  private final long panicDurationTicks;
  private @Nullable MatchPlayer fleeSource; // Used by both panicking and avoiding
  private @Nullable Path fleePath; // Used by both panicking and avoiding
  private long panicStartTick;
  private long panicExpiryTick = -1;
  private long nextRepathTick;
  private boolean isAvoiding;
  private final double avoidRangeSq;
  private final Filter avoidFilter;

  public EvadeInstance(Mannequin mannequin, EvadeBehavior behavior, Zombie ghost) {
    this.mannequin = mannequin;
    this.behavior = behavior;
    this.ghost = ghost;
    this.panicDurationTicks =
        (behavior.panicDuration() != null ? behavior.panicDuration() : DEFAULT_PANIC_DURATION)
                .toMillis()
            / 50;
    this.avoidRangeSq =
        behavior.avoidRange() != null ? behavior.avoidRange() * behavior.avoidRange() : -1;
    this.avoidFilter = behavior.avoidFilter();
  }

  public boolean matches(Entity entity) {
    return mannequin.getEntityId().equals(entity.getUniqueId());
  }

  public static boolean isPanicCause(EntityDamageEvent.DamageCause cause) {
    return PANIC_CAUSES.contains(cause);
  }

  public void startPanic(@Nullable MatchPlayer attacker, Tick now) {
    if (!behavior.panic()) return;
    this.fleeSource = attacker;
    this.panicExpiryTick = now.tick + panicDurationTicks;
    this.panicStartTick = now.tick + 6; // Delay panicking start so mannequin can take kb
    this.fleePath = null;
    this.nextRepathTick = 0; // Force repathing once delay ends
  }

  public boolean isPanicking(Tick now) {
    return now.tick < panicExpiryTick;
  }

  public boolean isEvading(Tick now) {
    return isPanicking(now) || isAvoiding;
  }

  public void tick(Match match, Tick now) {
    if (isPanicking(now)) {
      if (now.tick < panicStartTick) return;
      flee(match, now, 0.28, 8);
      return;
    }
    // Post panic cleanup
    if (panicExpiryTick != -1 && now.tick >= panicExpiryTick) {
      panicExpiryTick = -1;
    }
    // Fleeing due to avoid attribute
    if (avoidRangeSq < 0) {
      isAvoiding = false;
      return;
    }

    fleeSource = nearestAvoidTarget(match);
    isAvoiding = fleeSource != null;
    if (isAvoiding) {
      flee(match, now, 0.22, 12);
    } else {
      fleePath = null;
    }
  }

  private void flee(Match match, Tick now, double speed, int repathBase) {
    if (fleePath == null || fleePath.isDone() || now.tick >= nextRepathTick) {
      Vector fleePoint = pickFleePoint(match);
      var manLoc = mannequin.getLocation();
      ghost.setPos(manLoc.getX(), manLoc.getY(), manLoc.getZ());
      ghost.setOnGround(true);
      fleePath = ghost
          .getNavigation()
          .createPath(
              new BlockPos(fleePoint.getBlockX(), fleePoint.getBlockY(), fleePoint.getBlockZ()), 0);
      mannequin.getEntity().lookAt(fleePoint.toLocation(match.getWorld()), LookAnchor.EYES);
    }
    nextRepathTick = now.tick + repathBase + match.getRandom().nextInt(5);

    PathWalking.step(mannequin, fleePath, speed, false); // Sprint speed
  }

  private @Nullable MatchPlayer nearestAvoidTarget(Match match) {
    if (avoidRangeSq < 0) {
      return null;
    }
    var manLoc = mannequin.getLocation();
    MatchPlayer nearest = null;
    double eligible = avoidRangeSq;
    for (MatchPlayer player : match.getParticipants()) {
      var bukkit = player.getBukkit();
      if (bukkit == null || player.isDead()) continue;
      if (avoidFilter != null && !avoidFilter.query(player).isAllowed()) {
        continue;
      }

      double dis = bukkit.getLocation().distanceSquared(manLoc);
      if (dis <= eligible) {
        eligible = dis;
        nearest = player;
      }
    }
    return nearest;
  }

  private Vector pickFleePoint(Match match) {
    var manLoc = mannequin.getLocation();
    Vector away;
    if (fleeSource != null && fleeSource.getBukkit() != null) {
      away = manLoc.toVector().subtract(fleeSource.getBukkit().getLocation().toVector());
      if (away.lengthSquared() < 0.01) away = new Vector(1, 0, 0);
    } else {
      double angle = match.getRandom().nextDouble() * 2 * Math.PI;
      away = new Vector(Math.cos(angle), 0, Math.sin(angle));
    }

    away.setY(0).normalize();
    if (match.getRandom().nextDouble() < 0.15) away.multiply(-0.5);
    double dis = 3 + match.getRandom().nextDouble() * 2;
    double jit = (match.getRandom().nextDouble() - 0.5) * dis * 1.5;
    return manLoc
        .toVector()
        .add(away.multiply(dis))
        .add(new Vector(-away.getZ() * jit, 0, away.getX() * jit));
  }
}
