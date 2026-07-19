package tc.oc.pgm.platform.modern.modules.behavior.pathing;

import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.level.pathfinder.Path;
import org.bukkit.util.Vector;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.time.Tick;
import tc.oc.pgm.platform.modern.modules.mannequin.Mannequin;

public class PathingInstance {

  private enum Phase {
    WALKING,
    IDLING,
    STUCK,
    GAVE_UP,
    DONE
  }

  private final Mannequin mannequin;
  private final PathingBehavior behavior;
  private final Zombie ghost;
  private final List<PathingGoalBehavior> goals;
  private final Vector startPos;
  private int currentIndex = 0; // Each goal gets index, start always = 0
  private Phase phase = Phase.WALKING;
  private @Nullable Path currentPath;
  private long nextRepathTick = 0;
  private @Nullable Vector lastProgressPos;
  private long idleUntilTick = 0;
  private long lastProgressTick = 0;
  private long stuckSinceTick = -1;

  public PathingInstance(Mannequin mannequin, PathingBehavior behavior, Zombie ghost) {
    this.mannequin = mannequin;
    this.behavior = behavior;
    this.ghost = ghost;
    this.goals = behavior.getGoals();
    this.startPos = behavior.getStart() != null
        ? behavior.getStart()
        : mannequin.getLocation().toVector();
  }

  public void tick(Match match, Tick now) {
    switch (phase) {
      case WALKING -> tickWalking(match, now);
      case IDLING -> tickIdling(now);
      case DONE, GAVE_UP -> {}
    }
  }

  public void tickWalking(Match match, Tick now) {
    Vector target = currentIndex == 0 ? startPos : goals.get(currentIndex - 1).getDestination();

    if (disSq(mannequin.getLocation().toVector(), target) <= goalRadiusSq(currentIndex)) {
      if (currentIndex > 0) {
        PathingGoalBehavior goal = goals.get(currentIndex - 1);

        if (goal.getCompletionAction() != null) {
          goal.getCompletionAction().trigger(match);
        }

        long idleTicks = goal.getIdle() != null ? goal.getIdle().toMillis() / 50 : 0;
        idleUntilTick = now.tick + idleTicks;
      }
      currentPath = null;
      phase = Phase.IDLING;
      return;
    }

    if (now.tick >= nextRepathTick) {
      var loc = mannequin.getLocation();
      ghost.setPos(loc.getX(), loc.getY(), loc.getZ());
      ghost.setOnGround(true);
      currentPath = ghost
          .getNavigation()
          .createPath(
              new BlockPos((int) target.getX(), (int) target.getY(), (int) target.getZ()), 0);
      nextRepathTick = now.tick + 10;
    }
    stepAlongPath();
  }

  private double disSq(Vector a, Vector b) {
    double dx = a.getX() - b.getX(), dz = a.getZ() - b.getZ();
    return dx * dx + dz * dz;
  }

  private double goalRadiusSq(int index) {
    if (index == 0) return 1.5 * 1.5;
    Float r = goals.get(index - 1).getGoalRadius();
    return r != null ? r * r : 1.5 * 1.5;
  }

  private void tickIdling(Tick now) {
    if (now.tick < idleUntilTick) return;
    currentIndex++;
    if (currentIndex > behavior.getGoals().size()) {
      if (behavior.isLoop()) currentIndex = 0;
      else {
        phase = Phase.DONE;
        return;
      }
    }
    phase = Phase.WALKING;
  }

  private void stepAlongPath() {
    if (currentPath == null || currentPath.isDone()) return;
    if (!mannequin.getEntity().isOnGround()) return;
    var nodePos = currentPath.getNextNodePos();
    var loc = mannequin.getLocation();
    var direction = new org.bukkit.util.Vector(
        nodePos.getX() + 0.5 - loc.getX(), 0, nodePos.getZ() + 0.5 - loc.getZ());
    if (direction.lengthSquared() < 0.25) {
      currentPath.advance();
      return;
    }
    org.bukkit.util.Vector movement = direction.normalize().multiply(0.15);
    if (nodePos.getY() - loc.getY() > 0.5) movement.setY(0.42);
    mannequin.getEntity().setVelocity(movement);
  }
}
