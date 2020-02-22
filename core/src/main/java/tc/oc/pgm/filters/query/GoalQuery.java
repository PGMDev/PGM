package tc.oc.pgm.filters.query;

import tc.oc.pgm.goals.Goal;

/**
 * Currently, the only thing this class does is derive the Match from a Goal. In the future, we may
 * have filters that respond specifically to goal queries, but currently we do not.
 */
public class GoalQuery extends MatchQuery {

  public GoalQuery(Goal goal) {
    super(null, goal.getMatch());
  }
}
