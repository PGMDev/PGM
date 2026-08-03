package tc.oc.pgm.platform.modern.modules.behavior.pathing;

import java.util.ArrayList;
import java.util.List;
import org.bukkit.util.Vector;
import org.jspecify.annotations.Nullable;

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
