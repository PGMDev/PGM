package tc.oc.pgm.platform.modern.modules.behavior.pathing;

import java.util.List;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.level.pathfinder.Path;
import org.bukkit.util.Vector;
import org.jspecify.annotations.Nullable;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.time.Tick;
import tc.oc.pgm.platform.modern.modules.behavior.PathWalking;
import tc.oc.pgm.platform.modern.modules.mannequin.Mannequin;
import tc.oc.pgm.platform.modern.modules.mannequin.MannequinMatchModule;

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
  private int currentIndex; // Each goal gets an index assigned, starting position always = 0
  private int lastIndexReached;
  private Phase phase = Phase.WALKING;
  private @Nullable Path currentPath;
  private long nextRepathTick;
  private @Nullable Vector lastProgressPos;
  private long idleUntilTick;
  private long lastProgressTick;
  private boolean wasInterrupted;
  private final boolean openDoors;

  public PathingInstance(
      Mannequin mannequin, PathingBehavior behavior, Zombie ghost, boolean openDoors) {
    this.mannequin = mannequin;
    this.behavior = behavior;
    this.ghost = ghost;
    this.openDoors = openDoors;
    this.goals = behavior.goals();
    this.startPos = behavior.start() != null ? behavior.start() : mannequin.getSpawnPos();
  }

  public void pathingInterrupted() {
    wasInterrupted = true;
  }

  public void tick(Match match, Tick now) {
    if (wasInterrupted) {
      wasInterrupted = false;

      if (phase == Phase.WALKING || phase == Phase.IDLING) {
        currentIndex = lastIndexReached;
        currentPath = null;
        lastProgressPos = null; // Time in combat does not count towards time stuck
        phase = Phase.WALKING;
      }
    }
    switch (phase) {
      case WALKING -> tickWalking(match, now);
      case IDLING -> tickIdling(now);
      case DONE, GAVE_UP -> {}
    }
  }

  private void tickWalking(Match match, Tick now) {
    Vector target = currentIndex == 0 ? startPos : goals.get(currentIndex - 1).destination();

    // Vanilla pathfinding always floors x/z coordinates and walks to the center of the target
    // block.
    // Unfortunately mannequins have to mimic this in order to not get stuck when walking to goals.
    int blockX = (int) Math.floor(target.getX());
    int blockZ = (int) Math.floor(target.getZ());
    Vector adjusted = new Vector(blockX + 0.5, target.getY(), blockZ + 0.5);

    if (disSq(mannequin.getLocation().toVector(), adjusted) <= goalRadiusSq(currentIndex)) {
      if (currentIndex > 0) {
        PathingGoalBehavior goal = goals.get(currentIndex - 1);

        if (goal.completionAction() != null) {
          goal.completionAction().trigger(match);
        }

        long idleTicks = goal.idle() != null ? goal.idle().toMillis() / 50 : 0;
        idleUntilTick = now.tick + idleTicks;
      }
      currentPath = null;
      lastIndexReached = currentIndex;
      phase = Phase.IDLING;
      return;
    }

    if (currentPath == null || currentPath.isDone()) {
      if (now.tick >= nextRepathTick) {
        var loc = mannequin.getLocation();
        ghost.setPos(loc.getX(), loc.getY(), loc.getZ());
        ghost.setOnGround(true);
        currentPath =
            ghost.getNavigation().createPath(blockX, (int) Math.floor(target.getY()), blockZ, 0);
        nextRepathTick = now.tick + 10;
      }
    }

    PathWalking.step(
        mannequin,
        currentPath,
        0.15,
        openDoors); // Regular walk speed + has option to open doors while chasing

    var pos = mannequin.getLocation().toVector();
    if (lastProgressPos == null || pos.distanceSquared(lastProgressPos) > 0.25) {
      lastProgressPos = pos;
      lastProgressTick = now.tick;
    } else if (behavior.stuck() != null
        && now.tick - lastProgressTick >= behavior.stuck().after().toMillis() / 50) {
      handleStuck(match);
    }
  }

  private double disSq(Vector a, Vector b) {
    double dx = a.getX() - b.getX(), dz = a.getZ() - b.getZ();
    return dx * dx + dz * dz;
  }

  private double goalRadiusSq(int index) {
    if (index == 0) return 0.5 * 0.5;
    Float radius = goals.get(index - 1).goalRadius();
    return radius != null ? radius * radius : 0.5 * 0.5;
  }

  private void tickIdling(Tick now) {
    if (now.tick < idleUntilTick) return;
    currentIndex++;
    if (currentIndex > behavior.goals().size()) {
      if (behavior.loop()) currentIndex = 0;
      else {
        phase = Phase.DONE;
        return;
      }
    }
    phase = Phase.WALKING;
  }

  private void handleStuck(Match match) {
    StuckBehavior stuck = behavior.stuck();
    if (stuck.stuckAction() != null) {
      stuck.stuckAction().trigger(match);
    }
    if (stuck.despawn()) {
      match.needModule(MannequinMatchModule.class).singleDespawn(mannequin);
      return;
    }
    if (stuck.giveUp()) {
      phase = Phase.GAVE_UP;
      currentPath = null;
      return;
    }
    int resolved = resolveMoveTo(stuck);
    Vector dest = resolved == 0 ? startPos : goals.get(resolved - 1).destination();
    currentIndex = resolved;
    currentPath = null;
    lastProgressPos = null;
    if (stuck.method() == RelocationMethod.TELEPORT) {
      mannequin.teleport(
          dest.getX(), dest.getY(), dest.getZ(), mannequin.getYaw(), mannequin.getPitch());
    }
    phase = Phase.WALKING;
  }

  private int resolveMoveTo(StuckBehavior stuck) {
    // Move to a goal using its numeric index
    if (stuck.moveToIndex() != null) {
      return stuck.moveToIndex();
    }

    if (stuck.moveTo() == null) {
      return Math.max(0, currentIndex - 1);
    }

    // Move to a goal using an enum
    return switch (stuck.moveTo()) {
      case START -> 0;
      case END -> goals.size();
      case PREVIOUS -> Math.max(0, currentIndex - 1);
      case NEXT -> Math.min(currentIndex + 1, goals.size());
    };
  }

  public boolean isWalking() {
    return phase == Phase.WALKING;
  }
}
