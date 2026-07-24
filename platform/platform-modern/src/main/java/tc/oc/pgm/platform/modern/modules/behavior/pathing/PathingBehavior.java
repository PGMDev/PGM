package tc.oc.pgm.platform.modern.modules.behavior.pathing;

import java.util.ArrayList;
import java.util.List;
import org.jspecify.annotations.Nullable;
import org.bukkit.util.Vector;

public record PathingBehavior(
    @Nullable Vector start,
    boolean loop,
    @Nullable Float leash,
    List<PathingGoalBehavior> goals,
    @Nullable StuckBehavior stuck) {

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
}
