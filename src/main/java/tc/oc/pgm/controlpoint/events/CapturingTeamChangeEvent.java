package tc.oc.pgm.controlpoint.events;

import javax.annotation.Nullable;
import org.bukkit.event.HandlerList;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.controlpoint.ControlPoint;
import tc.oc.pgm.teams.Team;

public class CapturingTeamChangeEvent extends ControlPointEvent {
  private static final HandlerList handlers = new HandlerList();
  @Nullable private final Team oldTeam;
  @Nullable private final Team newTeam;

  public CapturingTeamChangeEvent(Match match, ControlPoint hill, Team oldTeam, Team newTeam) {
    super(match, hill);
    this.oldTeam = oldTeam;
    this.newTeam = newTeam;
  }

  public @Nullable Team getOldTeam() {
    return this.oldTeam;
  }

  public @Nullable Team getNewTeam() {
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
