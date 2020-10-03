package tc.oc.pgm.goals;

import javax.annotation.Nullable;
import tc.oc.pgm.api.party.Competitor;

/** A {@link Goal} that is completed gradually, and can report completion as a percentage */
public interface IncrementalGoal<T extends GoalDefinition> extends Goal<T> {
  /** Return the total completion percentage of this goal in the range 0..1 */
  double getCompletion();

  /**
   * Return the completion of this goal relative to the given competitor
   *
   * <p>If any goal implementing this has the same completion for all teams that can complete it
   * they should implement {@link IncrementalGoal#getCompletion()} only instead of overriding this
   *
   * <p>All competitors passed to this method must be checked with {@link
   * Goal#canComplete(Competitor)} to avoid returning any values for teams that are not supposed to
   * be able to complete this
   *
   * @param competitor a team that must be able to complete this goal
   * @return the completion percentage in the range 0..1
   */
  default double getCompletion(Competitor competitor) { // TODO Default implementation, @NUllable
    return getCompletion();
  }
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
