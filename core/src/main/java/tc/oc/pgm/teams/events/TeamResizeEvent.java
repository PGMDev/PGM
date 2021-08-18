package tc.oc.pgm.teams.events;

import com.google.common.base.Preconditions;
import org.bukkit.event.HandlerList;
import tc.oc.pgm.api.party.event.PartyEvent;
import tc.oc.pgm.teams.Team;

/** Fired after the maximum size of a team has been changed from the default value */
public class TeamResizeEvent extends PartyEvent {
  private final Team team;

  public TeamResizeEvent(Team team) {
    super(team);
    Preconditions.checkNotNull(team, "team");
    this.team = team;
  }

  public Team getTeam() {
    return this.team;
  }

  private static final HandlerList handlers = new HandlerList();

  @Override
  public HandlerList getHandlers() {
    return handlers;
  }

  public static HandlerList getHandlerList() {
    return handlers;
  }
}
