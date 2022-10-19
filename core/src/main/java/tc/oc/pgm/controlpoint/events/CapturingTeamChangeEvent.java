package tc.oc.pgm.controlpoint.events;

import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.Nullable;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.party.Competitor;
import tc.oc.pgm.controlpoint.ControlPoint;

public class CapturingTeamChangeEvent extends ControlPointEvent {
  private static final HandlerList handlers = new HandlerList();
  @Nullable private final Competitor oldTeam;
  @Nullable private final Competitor newTeam;

  public CapturingTeamChangeEvent(
      Match match, ControlPoint hill, Competitor oldTeam, Competitor newTeam) {
    super(match, hill);
    this.oldTeam = oldTeam;
    this.newTeam = newTeam;
  }

  public @Nullable Competitor getOldTeam() {
    return this.oldTeam;
  }

  public @Nullable Competitor getNewTeam() {
    return this.newTeam;
  }

  @Override
  public HandlerList getHandlers() {
    return handlers;
  }

  public static HandlerList getHandlerList() {
    return handlers;
  }
}
