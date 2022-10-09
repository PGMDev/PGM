package tc.oc.pgm.goals;

import org.jetbrains.annotations.Nullable;

/** A {@link Goal} that is completed gradually, and can report completion as a percentage */
public interface IncrementalGoal<T extends GoalDefinition> extends Goal<T> {
  /** Return the completion percentage of this goal in the range 0..1 */
  double getCompletion();

  /** Render a string representation of goal completion */
  String renderCompletion();

  /**
   * Render a precise representation of goal completion, or null if no precise format is supported
   */
  @Nullable
  String renderPreciseCompletion();

  /** True if partial completion should be visible to participating players */
  boolean getShowProgress();
}
