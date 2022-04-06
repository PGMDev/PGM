package tc.oc.pgm.goals.events;

import com.google.common.collect.ImmutableList;
import java.util.List;
import org.bukkit.event.HandlerList;
import tc.oc.pgm.api.goal.Goal;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.party.Competitor;
import tc.oc.pgm.goals.Contribution;

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

  private static final HandlerList handlers = new HandlerList();

  public static HandlerList getHandlerList() {
    return handlers;
  }

  @Override
  public HandlerList getHandlers() {
    return handlers;
  }
}
