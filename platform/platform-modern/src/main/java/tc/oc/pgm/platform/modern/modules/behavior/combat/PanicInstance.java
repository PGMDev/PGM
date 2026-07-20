package tc.oc.pgm.platform.modern.modules.behavior.combat;

import io.papermc.paper.entity.LookAnchor;
import java.time.Duration;
import java.util.Set;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.level.pathfinder.Path;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.util.Vector;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.api.time.Tick;
import tc.oc.pgm.platform.modern.modules.behavior.PathWalking;
import tc.oc.pgm.platform.modern.modules.mannequin.Mannequin;

public class PanicInstance {

  public static final Duration DEFAULT_PANIC_DURATION = Duration.ofSeconds(5);

  private final Mannequin mannequin;
  private final net.minecraft.world.entity.monster.zombie.Zombie ghost;
  private final long panicDurationTicks;
  private @Nullable MatchPlayer panicSource;
  private @Nullable Path panicPath;
  private long nextRepathTick;
  private long panicStartTick;
  private long panicExpiryTick;
  private boolean openDoors;

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

  public PanicInstance(Mannequin mannequin, CombatBehavior behavior, Zombie ghost) {
    this.mannequin = mannequin;
    this.ghost = ghost;
    this.panicDurationTicks =
        (behavior.getPanicDuration() != null ? behavior.getPanicDuration() : DEFAULT_PANIC_DURATION)
                .toMillis()
            / 50;
  }

  public void startPanic(@Nullable MatchPlayer attacker, Tick now) {
    this.panicSource = attacker;
    this.panicExpiryTick = now.tick + panicDurationTicks;
    this.panicStartTick = now.tick + 6; // Delay panicking start so mannequin can take kb
    this.panicPath = null;
    this.nextRepathTick = 0; // Force repathing once delay ends
  }

  public boolean isPanicking(Tick now) {
    return now.tick < panicExpiryTick;
  }

  public static boolean isPanicCause(EntityDamageEvent.DamageCause cause) {
    return PANIC_CAUSES.contains(cause);
  }

  public void tick(Match match, Tick now) {
    if (!isPanicking(now)) {
      if (panicSource != null) {
        panicSource = null;
        panicPath = null;
      }
      return;
    }
    if (now.tick < panicStartTick) return; // / Delay panicking start so mannequin can take kb

    if (now.tick >= nextRepathTick) {
      Vector panicPoint = pickPanicPoint(match);
      if (panicPoint != null) {
        var loc = mannequin.getLocation();
        ghost.setPos(loc.getX(), loc.getY(), loc.getZ());
        ghost.setOnGround(true);
        panicPath = ghost
            .getNavigation()
            .createPath(
                new BlockPos(
                    panicPoint.getBlockX(), panicPoint.getBlockY(), panicPoint.getBlockZ()),
                0);
        mannequin.getEntity().lookAt(panicPoint.toLocation(match.getWorld()), LookAnchor.EYES);
      }
      nextRepathTick = now.tick + 8 + match.getRandom().nextInt(5);
    }
    PathWalking.step(mannequin, panicPath, 0.28, openDoors);
  }

  private @Nullable Vector pickPanicPoint(Match match) {
    var manLoc = mannequin.getLocation();
    Vector away;
    if (panicSource != null && panicSource.getBukkit() != null) {
      away = manLoc.toVector().subtract(panicSource.getBukkit().getLocation().toVector());

      if (away.lengthSquared() < 0.01) {
        away = new Vector(1, 0, 0);
      }
    } else {
      double angle = match.getRandom().nextDouble() * 2 * Math.PI;
      away = new Vector(Math.cos(angle), 0, Math.sin(angle));
    }

    away.setY(0).normalize();
    if (match.getRandom().nextDouble() < 0.15) {
      away.multiply(-0.5);
    }

    double dis = 3 + match.getRandom().nextDouble() * 2;
    double jit = (match.getRandom().nextDouble() - 0.5) * dis * 1.5;
    Vector latOffset = new Vector(-away.getZ() * jit, 0, away.getX() * jit);
    return manLoc.toVector().add(away.multiply(dis)).add(latOffset);
  }
}
