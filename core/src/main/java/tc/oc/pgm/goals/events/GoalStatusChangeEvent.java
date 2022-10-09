package tc.oc.pgm.goals.events;

import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.Nullable;
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

  private static final HandlerList handlers = new HandlerList();

  public static HandlerList getHandlerList() {
    return handlers;
  }

  @Override
  public HandlerList getHandlers() {
    return handlers;
  }
}
