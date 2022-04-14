package tc.oc.pgm.filters.query;

import tc.oc.pgm.goals.Goal;
import tc.oc.pgm.goals.GoalDefinition;

/**
 * Currently, the only thing this class does is derive the Match from a Goal. In the future, we may
 * have filters that respond specifically to goal queries, but currently we do not.
 */
public class GoalQuery extends MatchQuery implements tc.oc.pgm.api.filter.query.GoalQuery {
  private final Goal<? extends GoalDefinition> goal;

  public GoalQuery(Goal<? extends GoalDefinition> goal) {
    super(null, goal.getMatch());
    this.goal = goal;
  }

  @Override
  public Goal<? extends GoalDefinition> getGoal() {
    return this.goal;
  }
}
