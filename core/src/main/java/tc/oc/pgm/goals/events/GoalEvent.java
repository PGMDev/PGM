package tc.oc.pgm.goals.events;

import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.Nullable;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.match.event.MatchEvent;
import tc.oc.pgm.api.party.Competitor;
import tc.oc.pgm.goals.Goal;

public abstract class GoalEvent extends MatchEvent {
  private final Goal goal;
  @Nullable private final Competitor competitor;

  protected GoalEvent(Match match, Goal goal, @Nullable Competitor competitor) {
    super(match);
    this.goal = goal;
    this.competitor = competitor;
  }

  protected GoalEvent(Goal goal, @Nullable Competitor competitor) {
    this(goal.getMatch(), goal, competitor);
  }

  public Goal getGoal() {
    return goal;
  }

  @Nullable
  public Competitor getCompetitor() {
    return competitor;
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
