package tc.oc.pgm.goals.events;

import com.google.common.collect.ImmutableList;
import java.util.List;
import lombok.Getter;
import org.bukkit.event.HandlerList;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.party.Competitor;
import tc.oc.pgm.goals.Contribution;
import tc.oc.pgm.goals.Goal;

@Getter
public class GoalCompleteEvent extends GoalEvent {

  /** True if the event was beneficial to the affected team, false if it was detrimental */
  private final boolean isGood;

  private final ImmutableList<? extends Contribution> contributions;

  public GoalCompleteEvent(Match match, Goal<?> goal, Competitor competitor, boolean isGood) {
    this(match, goal, competitor, isGood, ImmutableList.of());
  }

  public GoalCompleteEvent(
      Match match,
      Goal<?> goal,
      Competitor competitor,
      boolean isGood,
      List<? extends Contribution> contributions) {
    super(match, goal, competitor);
    this.isGood = isGood;
    this.contributions = ImmutableList.copyOf(contributions);
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
