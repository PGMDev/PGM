package tc.oc.pgm.api.filter.query;

import tc.oc.pgm.goals.Goal;
import tc.oc.pgm.goals.GoalDefinition;

public interface GoalQuery<T extends GoalDefinition> extends MatchQuery {
  Goal<T> getGoal();
}
