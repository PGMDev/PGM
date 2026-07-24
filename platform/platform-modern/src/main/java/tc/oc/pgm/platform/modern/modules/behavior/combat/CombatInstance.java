package tc.oc.pgm.platform.modern.modules.behavior.combat;

import io.papermc.paper.entity.LookAnchor;
import java.time.Duration;
import java.util.Map;
import org.jspecify.annotations.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.level.pathfinder.PathType;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.api.time.Tick;
import tc.oc.pgm.platform.modern.modules.behavior.PathWalking;
import tc.oc.pgm.platform.modern.modules.behavior.home.HomeBehavior;
import tc.oc.pgm.platform.modern.modules.mannequin.Mannequin;

public class CombatInstance {

  public static final Float DEFAULT_ATTACK_RANGE = 3.0f;
  public static final Duration DEFAULT_ATTACK_INTERVAL = Duration.ofSeconds(1);
  public static final Map<PathType, Float> DANGER_PENALTIES = Map.of(
      PathType.DAMAGE_OTHER, -1.0F,
      PathType.DANGER_OTHER, -1.0F,
      PathType.DAMAGE_FIRE, -1.0F,
      PathType.DANGER_FIRE, -1.0F,
      PathType.DAMAGE_CAUTIOUS, -1.0F,
      PathType.LAVA, -1.0F);

  private final Mannequin mannequin;
  private final CombatBehavior behavior;
  private final @Nullable HomeBehavior home;
  private @Nullable MatchPlayer target;
  private final boolean openDoors;
  private long aggroExpiryTick = -1;
  private long nextAttackTick;
  private final double rangeSq;
  private final long intervalTicks;
  private final Zombie ghost;
  private @Nullable Path currentPath;
  private long nextRepathTick;
  private final boolean hasPathing;
  private final double leashSq;
  private @Nullable Vector aggroAnchor;

  public CombatInstance(
      Mannequin mannequin,
      CombatBehavior behavior,
      @Nullable HomeBehavior home,
      boolean openDoors,
      Zombie ghost,
      boolean hasPathing,
      @Nullable Float leash) {
    this.mannequin = mannequin;
    this.behavior = behavior;
    this.home = home;
    this.openDoors = openDoors;
    this.rangeSq = behavior.range() * behavior.range();
    this.intervalTicks = behavior.interval().toMillis() / 50;
    this.ghost = ghost;
    this.hasPathing = hasPathing;
    this.leashSq = leash != null ? leash * leash : -1;
  }

  public boolean matches(Entity entity) {
    return mannequin.getEntityId().equals(entity.getUniqueId());
  }

  public void onAttacked(MatchPlayer attacker, Tick now) {
    // Handle neutral/hostile. Passive is handled in EvadeInstance
    if (behavior.hostility() == HostilityType.PASSIVE) return;

    if (target == null || target == attacker) {
      target = attacker;
      refreshExpiry(now);
      // Pathing creates a home equiv around aggroAnchor with leash as the radius
      if (hasPathing) {
        aggroAnchor = mannequin.getLocation().toVector();
      }
    }
  }

  public void tick(Match match, Tick now, boolean canMove) {
    validateTarget(now);

    if (target == null && behavior.hostility() == HostilityType.HOSTILE) {
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

    if (canMove && target != null && !inAttackRange(target)) {
      Player bukkit = target.getBukkit();
      if (bukkit != null && !(hasPathing && leashSq < 0)) {
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
          nextRepathTick = now.tick + 8;
        }

        PathWalking.step(
            mannequin,
            currentPath,
            0.15,
            openDoors); // Regular walk speed + has option to open doors while chasing
        mannequin.getEntity().lookAt(bukkit.getLocation(), LookAnchor.EYES);
      }
    }
  }

  private void validateTarget(Tick now) {
    if (target == null) return;
    Player bukkit = target.getBukkit();
    if (bukkit == null
        || target.isDead()
        || !target.isParticipating()
        || aggroExpiryTick != -1 && now.tick > aggroExpiryTick
        || outsideHome(bukkit)
        || beyondLeash()) {
      target = null;
      aggroExpiryTick = -1;
    }
  }

  private void acquireTarget(Match match, Tick now) {
    if (home == null && !hasPathing) {
      return;
    }
    for (MatchPlayer player : match.getParticipants()) {
      Player bukkit = player.getBukkit();
      if (bukkit == null || player.isDead()) continue;
      boolean inRange = hasPathing
          ? bukkit.getLocation().distanceSquared(mannequin.getLocation()) <= rangeSq * 5
          // Give rangeSq a buffer for aggro visibility for aggro stuttering at home bounds
          : home.region().contains(bukkit.getLocation());

      if (inRange) {
        target = player;
        if (hasPathing) {
          aggroAnchor = mannequin.getLocation().toVector();
        }
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
        behavior.duration() != null ? now.tick + behavior.duration().toMillis() / 50 : -1;
  }

  private boolean inAttackRange(MatchPlayer target) {
    Player bukkit = target.getBukkit();
    if (bukkit == null) return false;
    return bukkit.getWorld().equals(mannequin.getEntity().getWorld())
        && bukkit.getLocation().distanceSquared(mannequin.getEntity().getLocation()) <= rangeSq;
  }

  private boolean beyondLeash() {
    if (!hasPathing || leashSq < 0 || aggroAnchor == null) return false;
    var pos = mannequin.getLocation().toVector();
    double dx = pos.getX() - aggroAnchor.getX();
    double dz = pos.getZ() - aggroAnchor.getZ();
    return dx * dx + dz * dz > leashSq;
  }

  private boolean outsideHome(Player bukkit) {
    if (home == null) {
      return false;
    }
    return !home.region().contains(bukkit.getLocation());
  }
}
