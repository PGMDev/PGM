package tc.oc.pgm.goals;

import org.jetbrains.annotations.Nullable;
import tc.oc.pgm.teams.TeamFactory;

public abstract class ProximityGoalDefinition extends OwnedGoalDefinition {
  private final @Nullable ProximityMetric preTouchMetric;
  private final @Nullable ProximityMetric postTouchMetric;

  public ProximityGoalDefinition(
      @Nullable String id,
      String name,
      @Nullable Boolean required,
      ShowOptions showOptions,
      TeamFactory owner,
      @Nullable ProximityMetric preTouchMetric,
      @Nullable ProximityMetric postTouchMetric) {
    super(id, name, required, showOptions, owner);
    this.preTouchMetric = preTouchMetric;
    this.postTouchMetric = postTouchMetric;
  }

  public ProximityGoalDefinition(
      @Nullable String id,
      String name,
      @Nullable Boolean required,
      ShowOptions showOptions,
      TeamFactory owner,
      @Nullable ProximityMetric preTouchMetric) {
    this(id, name, required, showOptions, owner, preTouchMetric, null);
  }

  public @Nullable ProximityMetric getPreTouchMetric() {
    return this.preTouchMetric;
  }

  public @Nullable ProximityMetric getPostTouchMetric() {
    return postTouchMetric;
  }
}
