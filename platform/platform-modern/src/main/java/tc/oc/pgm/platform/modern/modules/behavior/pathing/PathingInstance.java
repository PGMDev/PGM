package tc.oc.pgm.platform.modern.modules.behavior.pathing;

import java.util.ArrayList;
import java.util.Collections;
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
  private final boolean randomGoal;
  private int loopsCompleted;
  private boolean forward = true;
  private boolean firstVisit = true;

  public PathingInstance(
      Mannequin mannequin, PathingBehavior behavior, Zombie ghost, boolean openDoors, Match match) {
    this.mannequin = mannequin;
    this.behavior = behavior;
    this.ghost = ghost;
    this.openDoors = openDoors;
    this.goals = new ArrayList<>(behavior.goals());
    this.startPos = behavior.start() != null ? behavior.start() : mannequin.getSpawnPos();
    this.randomGoal = behavior.loop() != null && behavior.loop().randomGoal();
    if (randomGoal) {
      Collections.shuffle(this.goals, match.getRandom());
    }
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
      case IDLING -> tickIdling(match, now);
      case DONE, GAVE_UP -> {}
    }
  }

  private void tickWalking(Match match, Tick now) {
    Vector target = currentIndex == 0 ? startPos : goals.get(currentIndex - 1).destination();

    // Vanilla pathfinding always floors x/z coords and walks to the center of the target block.
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
      } else {
        // Suppress a start node's completion-action when a mannequin is first spawned
        firstVisit = false;
      }

      currentPath = null;
      lastIndexReached = currentIndex;
      phase = Phase.IDLING;
      return;
    }

    if (currentPath == null || currentPath.isDone()) {
      if (now.tick >= nextRepathTick) {
        var manLoc = mannequin.getLocation();
        ghost.setPos(manLoc.getX(), manLoc.getY(), manLoc.getZ());
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

  private void tickIdling(Match match, Tick now) {
    if (now.tick < idleUntilTick) return;
    if (forward) {
      currentIndex++;
    } else {
      currentIndex--;
    }

    LoopBehavior loop = behavior.loop();
    if (currentIndex > goals.size()) {
      if (loop == null) {
        phase = Phase.DONE;
        return;
      }

      if (loop.reverse()) {
        forward = false;
        currentIndex = goals.size() - 1;
      } else {
        if (loopCompletion(match, loop)) return;
        if (randomGoal) {
          Collections.shuffle(this.goals, match.getRandom());

          // TODO: come back and fix returning to start goal everytime a full shuffle is walked

        } else {
          currentIndex = 0;
        }
      }
    } else if (currentIndex < 0) {
      if (loop == null) {
        phase = Phase.DONE;
        return;
      }
      if (loopCompletion(match, loop)) return;

      forward = true;
      currentIndex = 0;
    }

    phase = Phase.WALKING;
  }

  private void handleStuck(Match match) {
    StuckBehavior stuck = behavior.stuck();
    if (stuck == null) {
      return;
    }
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

  private boolean loopCompletion(Match match, LoopBehavior loop) {
    loopsCompleted++;

    // Completion action fired when looping back and returning to start node
    if (loop.completionAction() != null) {
      loop.completionAction().trigger(match);
    }

    if (loop.times() != null && loopsCompleted >= loop.times()) {
      phase = Phase.DONE;
      return true;
    }
    return false;
  }
}
