package tc.oc.pgm.api.filter.query;

import tc.oc.pgm.api.goal.Goal;
import tc.oc.pgm.api.goal.GoalDefinition;

public interface GoalQuery<T extends GoalDefinition> extends MatchQuery {
  Goal<T> getGoal();
}
