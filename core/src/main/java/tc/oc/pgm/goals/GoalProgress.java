package tc.oc.pgm.goals;

import com.google.common.collect.ImmutableList;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import javax.annotation.Nonnull;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.party.Competitor;

/** A measurement of progress towards completing goals in a match */
public class GoalProgress implements Comparable<GoalProgress> {
  public final int completed; // Number of completed goals
  public final int touched; // Number of completed goals
  public final ImmutableList<Double>
      progress; // Progress of incremental goals, from highest to lowest
  public final ImmutableList<Integer>
      completionProximity; // Distance squared to untouched goals, from lowest to highest
  public final ImmutableList<Integer>
      touchProximity; // Distance squared to touched goals, from lowest to highest

  private static <T extends Collection<?>> T shorter(T a, T b) {
    return a.size() < b.size() ? a : b;
  }

  private static int compareProgresses(List<Double> a, List<Double> b) {
    int count = Math.max(a.size(), b.size());
    double aProgress = 0, bProgress = 0;

    for (int i = 0; i < count; i++) {
      aProgress = i < a.size() ? a.get(i) : 0;
      bProgress = i < b.size() ? b.get(i) : 0;
      if (aProgress != bProgress) break; // Find first differing progress
    }

    return Double.compare(bProgress, aProgress);
  }

  /**
   * Only compare as many proximity scores as both teams have available. If one team has more
   * proximity scores available than the other, ignore the extras.
   */
  private static int compareProximities(List<Integer> a, List<Integer> b) {
    int count = Math.min(a.size(), b.size());
    int aProximity = Integer.MAX_VALUE, bProximity = Integer.MAX_VALUE;

    for (int i = 0; i < count; i++) {
      aProximity = a.get(i);
      bProximity = b.get(i);
      if (aProximity != bProximity) break;
    }

    return Integer.compare(aProximity, bProximity);
  }

  private GoalProgress(
      int completed,
      int touched,
      Collection<Double> progress,
      Collection<Integer> completionProximity,
      Collection<Integer> touchProximity) {
    this.completed = completed;
    this.touched = touched;
    this.progress = ImmutableList.copyOf(progress);
    this.completionProximity = ImmutableList.copyOf(completionProximity);
    this.touchProximity = ImmutableList.copyOf(touchProximity);
  }

  GoalProgress(Competitor competitor) {
    Match match = competitor.getMatch();

    int completed = 0;
    int touched = 0;
    List<Double> progress = new ArrayList<>();
    List<Integer> completionProximity = new ArrayList<>();
    List<Integer> touchProximity = new ArrayList<>();
    final GoalMatchModule gmm = match.needModule(GoalMatchModule.class);

    for (Goal<?> goal : gmm.getGoals(competitor)) {
      if (goal.isRequired()) {
        if (goal.isCompleted(competitor)) {
          completed++;
        } else {
          if (goal instanceof ProximityGoal) {
            ProximityGoal proximity = (ProximityGoal) goal;
            TouchableGoal touchable = goal instanceof TouchableGoal ? (TouchableGoal) goal : null;

            if (touchable != null && touchable.hasTouched(competitor)) {
              touched++;
              if (proximity.isProximityRelevant(competitor)) {
                completionProximity.add(proximity.getProximity(competitor));
              }
            } else {
              if (proximity.isProximityRelevant(competitor)) {
                touchProximity.add(proximity.getProximity(competitor));
              }
            }

            if (goal instanceof IncrementalGoal) {
              IncrementalGoal incrementalGoal = (IncrementalGoal) goal;
              progress.add(incrementalGoal.getCompletion());
            } else if (touchable != null && touchable.hasTouched(competitor)) {
              // A touched, non-incremental goal is worth 50% completion
              progress.add(0.5);
            }
          }
        }
      }
    }

    Collections.sort(progress, Collections.reverseOrder());
    Collections.sort(completionProximity);
    Collections.sort(touchProximity);

    this.completed = completed;
    this.touched = touched;
    this.progress = ImmutableList.copyOf(progress);
    this.completionProximity = ImmutableList.copyOf(completionProximity);
    this.touchProximity = ImmutableList.copyOf(touchProximity);
  }

  @Override
  public int compareTo(@Nonnull GoalProgress that) {
    // This team has more completed goals, so they take the lead
    if (this.completed > that.completed) return -1;
    if (this.completed < that.completed) return 1;

    // Equal number of completed goals, compare touches
    if (this.touched > that.touched) return -1;
    if (this.touched < that.touched) return 1;

    // Equal number of touches, compare the progress of incremental goals from highest to lowest
    int compareProgress = compareProgresses(this.progress, that.progress);
    if (compareProgress != 0) return compareProgress;

    // All progresses are equal, compare proximity to a completion
    int compareCompletionProximity =
        compareProximities(this.completionProximity, that.completionProximity);
    if (compareCompletionProximity != 0) return compareCompletionProximity;

    // This team got equally close to another completion, compare proximity to touch
    int compareTouchProximity = compareProximities(this.touchProximity, that.touchProximity);
    if (compareTouchProximity != 0) return compareTouchProximity;

    // Both teams are equal in every measurable respect
    return 0;
  }

  @Override
  public boolean equals(Object obj) {
    return obj instanceof GoalProgress && this.compareTo((GoalProgress) obj) == 0;
  }

  /**
   * Given two tied sets of accomplishments, return the common subset of accomplishments that can be
   * used to compare the two sets.
   */
  public static GoalProgress commonSubset(GoalProgress a, GoalProgress b) {
    return new GoalProgress(
        a.touched,
        a.completed,
        a.progress,
        shorter(a.completionProximity, b.completionProximity),
        shorter(a.touchProximity, b.touchProximity));
  }
}
