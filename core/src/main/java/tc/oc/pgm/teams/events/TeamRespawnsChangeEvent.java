package tc.oc.pgm.teams.events;

import org.bukkit.event.HandlerList;
import tc.oc.pgm.api.party.event.PartyEvent;
import tc.oc.pgm.teams.Team;

public class TeamRespawnsChangeEvent extends PartyEvent {
  private final Team team;
  private final int from;
  private final int to;

  private static final HandlerList handlers = new HandlerList();

  public TeamRespawnsChangeEvent(Team team, int from, int to) {
    super(team);
    this.team = team;
    this.from = from;
    this.to = to;
  }

  public Team getTeam() {
    return this.team;
  }

  public int getFrom() {
    return this.from;
  }

  public int getTo() {
    return this.to;
  }

  @Override
  public HandlerList getHandlers() {
    return handlers;
  }

  public static HandlerList getHandlerList() {
    return handlers;
  }
}
