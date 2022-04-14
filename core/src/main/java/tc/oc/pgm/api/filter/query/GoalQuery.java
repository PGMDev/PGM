package tc.oc.pgm.api.filter.query;

import tc.oc.pgm.goals.Goal;
import tc.oc.pgm.goals.GoalDefinition;

public interface GoalQuery extends MatchQuery {
  Goal<? extends GoalDefinition> getGoal();
}
