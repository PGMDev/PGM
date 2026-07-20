package tc.oc.pgm.platform.modern.modules.behavior.pathing;

import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nullable;
import org.bukkit.util.Vector;

public class PathingBehavior {
  private final @Nullable Vector start;
  private final boolean loop;
  private final @Nullable Float leash;
  private final List<PathingGoalBehavior> goals;
  private final @Nullable StuckBehavior stuck;

  public PathingBehavior(
      @Nullable Vector start,
      boolean loop,
      @Nullable Float leash,
      List<PathingGoalBehavior> goals,
      @Nullable StuckBehavior stuck) {
    this.start = start;
    this.loop = loop;
    this.leash = leash;
    this.goals = new ArrayList<>(goals);
    this.stuck = stuck;
  }

  public @Nullable Vector getStart() {
    return start;
  }

  public boolean isLoop() {
    return loop;
  }

  public @Nullable Float getLeash() {
    return leash;
  }

  public List<PathingGoalBehavior> getGoals() {
    return goals;
  }

  public @Nullable StuckBehavior getStuck() {
    return stuck;
  }
}
