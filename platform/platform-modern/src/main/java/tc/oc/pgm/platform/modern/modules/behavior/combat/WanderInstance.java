package tc.oc.pgm.platform.modern.modules.behavior.combat;

import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.level.pathfinder.Path;
import org.bukkit.util.Vector;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.time.Tick;
import tc.oc.pgm.platform.modern.modules.behavior.PathWalking;
import tc.oc.pgm.platform.modern.modules.mannequin.Mannequin;

public class WanderInstance {

  private final Mannequin mannequin;
  private final CombatBehavior behavior;
  private final net.minecraft.world.entity.monster.zombie.Zombie ghost;
  private @Nullable Path wanderPath;
  private long nextWanderTick;
  private Vector lastWanderProgressPos;
  private long lastWanderProgressTick;

  public WanderInstance(Mannequin mannequin, CombatBehavior behavior, Zombie ghost) {
    this.mannequin = mannequin;
    this.behavior = behavior;
    this.ghost = ghost;
  }

  public @Nullable Path getWanderPath() {
    return wanderPath;
  }

  public void clearWanderPath() {
    this.wanderPath = null;
  }

  public void tick(Match match, Tick now) {
    if (wanderPath == null || wanderPath.isDone()) {
      if (now.tick >= nextWanderTick) {
        Vector point = pickWanderPoint(match);
        if (point != null) {
          var loc = mannequin.getLocation();
          ghost.setPos(loc.getX(), loc.getY(), loc.getZ());
          ghost.setOnGround(true);

          wanderPath = ghost
              .getNavigation()
              .createPath(new BlockPos(point.getBlockX(), point.getBlockY(), point.getBlockZ()), 0);
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
        wanderPath = null;
        nextWanderTick = now.tick + randomIdleTicks(match);
        return;
      }
      PathWalking.step(mannequin, wanderPath, 0.15);
    }
  }

  private @Nullable Vector pickWanderPoint(Match match) {
    if (behavior.getHome() == null) {
      return null;
    }

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
}
