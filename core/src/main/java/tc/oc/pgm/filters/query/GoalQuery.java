package tc.oc.pgm.filters.query;

import tc.oc.pgm.goals.Goal;
import tc.oc.pgm.goals.GoalDefinition;

/**
 * Currently, the only thing this class does is derive the Match from a Goal. In the future, we may
 * have filters that respond specifically to goal queries, but currently we do not.
 */
public class GoalQuery<T extends GoalDefinition> extends MatchQuery
    implements tc.oc.pgm.api.filter.query.GoalQuery<T> {
  private final Goal<T> goal;

  public GoalQuery(Goal<T> goal) {
    super(null, goal.getMatch());
    this.goal = goal;
  }

  @Override
  public Goal<T> getGoal() {
    return this.goal;
  }
}
