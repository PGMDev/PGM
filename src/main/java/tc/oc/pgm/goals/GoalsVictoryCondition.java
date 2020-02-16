package tc.oc.pgm.goals;

import java.util.Collection;
import java.util.Map;
import tc.oc.component.Component;
import tc.oc.component.types.PersonalizedText;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.party.Competitor;
import tc.oc.pgm.result.VictoryCondition;

// TODO: Break this down into multiple chainable conditions i.e. completions, touches, proximity,
// etc.
public class GoalsVictoryCondition implements VictoryCondition {

  @Override
  public Priority getPriority() {
    return Priority.GOALS;
  }

  @Override
  public boolean isFinal(Match match) {
    return false;
  }

  @Override
  public int compare(Competitor a, Competitor b) {
    GoalMatchModule gmm = a.getMatch().needModule(GoalMatchModule.class);
    return gmm.getProgress(a).compareTo(gmm.getProgress(b));
  }

  @Override
  public boolean isCompleted(Match match) {
    GoalMatchModule gmm = match.needModule(GoalMatchModule.class);
    competitors:
    for (Map.Entry<Competitor, Collection<Goal>> entry :
        gmm.getGoalsByCompetitor().asMap().entrySet()) {
      boolean someRequired = false;
      for (Goal<?> goal : entry.getValue()) {
        if (goal.isRequired()) {
          // If any required goals are incomplete, skip to the next competitor
          if (!goal.isCompleted(entry.getKey())) continue competitors;
          someRequired = true;
        }
      }
      // If some goals are required, and they are all complete, competitor wins the match
      if (someRequired) return true;
    }
    // If no competitors won, match is not over
    return false;
  }

  @Override
  public Component getDescription(Match match) {
    return new PersonalizedText("most objectives");
  }
}
