package tc.oc.pgm.controlpoint.events;

import javax.annotation.Nullable;
import org.bukkit.event.HandlerList;
import tc.oc.pgm.controlpoint.ControlPoint;
import tc.oc.pgm.match.Match;
import tc.oc.pgm.teams.Team;

public class ControllerChangeEvent extends ControlPointEvent {
  private static final HandlerList handlers = new HandlerList();
  @Nullable private final Team oldController;
  @Nullable private final Team newController;

  public ControllerChangeEvent(
      Match match, ControlPoint hill, Team oldController, Team newController) {
    super(match, hill);
    this.oldController = oldController;
    this.newController = newController;
  }

  @Override
  public HandlerList getHandlers() {
    return handlers;
  }

  public static HandlerList getHandlerList() {
    return handlers;
  }

  @Nullable
  public Team getNewController() {
    return newController;
  }

  @Nullable
  public Team getOldController() {
    return oldController;
  }
}
