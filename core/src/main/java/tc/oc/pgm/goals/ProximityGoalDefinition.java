package tc.oc.pgm.goals;

import javax.annotation.Nullable;
import tc.oc.pgm.teams.TeamFactory;

public abstract class ProximityGoalDefinition extends OwnedGoalDefinition {
  private final @Nullable ProximityMetric preTouchMetric;
  private final @Nullable ProximityMetric postTouchMetric;

  public ProximityGoalDefinition(
      @Nullable String id,
      String name,
      @Nullable Boolean required,
      boolean visible,
      TeamFactory owner,
      @Nullable ProximityMetric preTouchMetric,
      @Nullable ProximityMetric postTouchMetric) {
    super(id, name, required, visible, owner);
    this.preTouchMetric = preTouchMetric;
    this.postTouchMetric = postTouchMetric;
  }

  public ProximityGoalDefinition(
      @Nullable String id,
      String name,
      @Nullable Boolean required,
      boolean visible,
      TeamFactory owner,
      @Nullable ProximityMetric preTouchMetric) {
    this(id, name, required, visible, owner, preTouchMetric, null);
  }

  public @Nullable ProximityMetric getPreTouchMetric() {
    return this.preTouchMetric;
  }

  public @Nullable ProximityMetric getPostTouchMetric() {
    return postTouchMetric;
  }
}
