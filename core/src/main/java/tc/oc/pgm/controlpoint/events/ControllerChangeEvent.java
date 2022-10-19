package tc.oc.pgm.controlpoint.events;

import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.Nullable;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.party.Competitor;
import tc.oc.pgm.controlpoint.ControlPoint;

public class ControllerChangeEvent extends ControlPointEvent {
  private static final HandlerList handlers = new HandlerList();
  @Nullable private final Competitor oldController;
  @Nullable private final Competitor newController;

  public ControllerChangeEvent(
      Match match, ControlPoint hill, Competitor oldController, Competitor newController) {
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
  public Competitor getNewController() {
    return newController;
  }

  @Nullable
  public Competitor getOldController() {
    return oldController;
  }
}
