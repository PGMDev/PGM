package tc.oc.pgm.platform.modern.modules.behavior.pathing;

import org.bukkit.util.Vector;
import org.jetbrains.annotations.Nullable;
import tc.oc.pgm.action.Action;
import tc.oc.pgm.api.match.Match;

import java.time.Duration;

public class PathingGoalBehavior {
  private final Vector destination;
  private final Duration idle;
  private final @Nullable Action<? super Match> completionAction;
  private final @Nullable Float goalRadius;

  public PathingGoalBehavior(
      Vector destination,
      Duration idle,
      @Nullable Action<? super Match> completionAction,
      @Nullable Float goalRadius) {
    this.destination = destination;
    this.idle = idle;
    this.completionAction = completionAction;
    this.goalRadius = goalRadius;
  }

  public Vector getDestination() {
    return destination;
  }

  public Duration getIdle() {
    return idle;
  }

  public @Nullable Action<? super Match> getCompletionAction() {
    return completionAction;
  }

  public @Nullable Float getGoalRadius() {
    return  goalRadius;
  }
}
