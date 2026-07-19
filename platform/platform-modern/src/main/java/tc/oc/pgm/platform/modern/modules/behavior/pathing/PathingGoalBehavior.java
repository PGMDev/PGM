package tc.oc.pgm.platform.modern.modules.behavior.pathing;

import java.time.Duration;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.Nullable;
import tc.oc.pgm.action.Action;
import tc.oc.pgm.api.match.Match;

public class PathingGoalBehavior {
  private final Vector destination;
  private final @Nullable Duration idle;
  private final @Nullable Action<? super Match> completionAction;
  private final @Nullable Float goalRadius;

  public PathingGoalBehavior(
      Vector destination,
      @Nullable Duration idle,
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

  public @Nullable Duration getIdle() {
    return idle;
  }

  public @Nullable Action<? super Match> getCompletionAction() {
    return completionAction;
  }

  public @Nullable Float getGoalRadius() {
    return goalRadius;
  }
}
