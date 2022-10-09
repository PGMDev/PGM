package tc.oc.pgm.goals.events;

import org.bukkit.Location;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.Nullable;
import tc.oc.pgm.api.party.Competitor;
import tc.oc.pgm.goals.ProximityGoal;

public class GoalProximityChangeEvent extends GoalEvent {
  private final @Nullable Location location;
  private final double oldDistance;
  private final double newDistance;

  public GoalProximityChangeEvent(
      ProximityGoal goal,
      Competitor team,
      @Nullable Location location,
      double oldDistance,
      double newDistance) {
    super(goal.getMatch(), goal, team);
    this.location = location;
    this.oldDistance = oldDistance;
    this.newDistance = newDistance;
  }

  public @Nullable Location getLocation() {
    return this.location;
  }

  public double getOldDistance() {
    return this.oldDistance;
  }

  public double getNewDistance() {
    return this.newDistance;
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
