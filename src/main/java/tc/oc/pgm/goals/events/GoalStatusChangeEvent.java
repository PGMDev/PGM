package tc.oc.pgm.goals.events;

import javax.annotation.Nullable;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.goals.Goal;
import tc.oc.pgm.teams.Team;

public class GoalStatusChangeEvent extends GoalEvent {

  public GoalStatusChangeEvent(Match match, Goal goal, @Nullable Team team) {
    super(match, goal, team);
  }

  public GoalStatusChangeEvent(Match match, Goal goal) {
    this(match, goal, null);
  }
}
