package tc.oc.pgm.goals.events;

import com.google.common.collect.ImmutableList;
import java.util.List;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.party.Competitor;
import tc.oc.pgm.goals.Contribution;
import tc.oc.pgm.goals.Goal;

public class GoalCompleteEvent extends GoalEvent {

  private final boolean isGood;
  private final ImmutableList<? extends Contribution> contributions;

  public GoalCompleteEvent(Match match, Goal goal, Competitor competitor, boolean isGood) {
    this(match, goal, competitor, isGood, ImmutableList.<Contribution>of());
  }

  public GoalCompleteEvent(
      Match match,
      Goal goal,
      Competitor competitor,
      boolean isGood,
      List<? extends Contribution> contributions) {
    super(match, goal, competitor);
    this.isGood = isGood;
    this.contributions = ImmutableList.copyOf(contributions);
  }

  public ImmutableList<? extends Contribution> getContributions() {
    return contributions;
  }

  /** @return true if the event was beneficial to the affected team, false if it was detrimental */
  public boolean isGood() {
    return isGood;
  }
}
