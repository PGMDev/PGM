package tc.oc.pgm.kits;

import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.teams.TeamFactory;
import tc.oc.pgm.teams.TeamMatchModule;

public class TeamSwitchKit extends DelayedKit {

  private final TeamFactory team;
  private final boolean showTitle;

  public TeamSwitchKit(TeamFactory team, boolean showTitle) {
    this.team = team;
    this.showTitle = showTitle;
  }

  @Override
  public void applyDelayed(MatchPlayer player, boolean force) {
    TeamMatchModule tmm = player.getMatch().needModule(TeamMatchModule.class);
    tmm.setTeamSwitchKit(player, showTitle, true);
    tmm.forceJoin(player, tmm.getTeam(team));
  }
}
