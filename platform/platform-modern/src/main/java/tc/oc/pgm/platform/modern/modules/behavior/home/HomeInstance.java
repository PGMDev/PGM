package tc.oc.pgm.platform.modern.modules.behavior.home;

import org.jspecify.annotations.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.level.pathfinder.Path;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.time.Tick;
import tc.oc.pgm.platform.modern.modules.behavior.PathWalking;
import tc.oc.pgm.platform.modern.modules.mannequin.Mannequin;

public class HomeInstance {

  private final Mannequin mannequin;
  private final HomeBehavior behavior;
  private final Zombie ghost;
  private final double straySq;
  private final long returnAfterTicks;
  private final @Nullable WanderInstance wander;
  private long idleSinceTick = -1;
  private @Nullable Path currentPath;
  private long nextRepathTick;

  public HomeInstance(Mannequin mannequin, HomeBehavior behavior, Zombie ghost) {
    this.mannequin = mannequin;
    this.behavior = behavior;
    this.ghost = ghost;
    this.straySq =
        behavior.strayDis() != null ? behavior.strayDis() * behavior.strayDis() : -1;
    this.returnAfterTicks =
        behavior.returnAfter() != null ? behavior.returnAfter().toMillis() / 50 : 0;
    this.wander = behavior.wander() ? new WanderInstance(mannequin, behavior, ghost) : null;
  }

  private boolean atHome(Match match) {
    if (straySq >= 0) {
      var center = behavior.region().getStatic(match).getBounds().getCenterPoint();
      var loc = mannequin.getLocation();
      double dx = center.getX() - loc.getX();
      double dz = center.getZ() - loc.getZ();
      return dx * dx + dz * dz <= straySq;
    }
    return behavior.region().contains(mannequin.getEntity());
  }

  public boolean returnsHome() {
    return behavior.strayDis() != null;
  }

  public void clearPaths() {
    currentPath = null;
    if (wander != null) {
      wander.clearWanderPath();
    }
  }

  public void tick(Match match, Tick now) {
    if (atHome(match)) {
      idleSinceTick = -1;
      currentPath = null;
      if (wander != null) {
        wander.tick(match, now);
      }
      return;
    }

    if ((!returnsHome())) return;
    if (idleSinceTick == -1) idleSinceTick = now.tick;
    if (now.tick - idleSinceTick >= returnAfterTicks) return;

    if (currentPath == null || currentPath.isDone()) {
      if (now.tick >= nextRepathTick) {
        var center = behavior.region().getStatic(match).getBounds().getCenterPoint();
        var loc = mannequin.getLocation();
        ghost.setPos(loc.getX(), loc.getY(), loc.getZ());
        ghost.setOnGround(true);
        currentPath = ghost
            .getNavigation()
            .createPath(
                new BlockPos((int) Math.floor(center.getX()), (int) Math.floor(center.getY()), (int)
                    Math.floor(center.getZ())),
                0);
        nextRepathTick = now.tick + 10;
      }
    }

    PathWalking.step(mannequin, currentPath, 0.15, false); // Regular walk speed
  }
}
