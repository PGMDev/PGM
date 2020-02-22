package tc.oc.pgm.teams.events;

import org.bukkit.event.HandlerList;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.match.event.MatchEvent;
import tc.oc.pgm.teams.Team;

public class TeamRespawnsChangeEvent extends MatchEvent {
  private final Team team;
  private final int from;
  private final int to;

  private static final HandlerList handlers = new HandlerList();

  public TeamRespawnsChangeEvent(Match match, Team team, int from, int to) {
    super(match);
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
