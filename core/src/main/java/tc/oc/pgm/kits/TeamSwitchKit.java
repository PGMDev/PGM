package tc.oc.pgm.kits;

import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.teams.TeamFactory;
import tc.oc.pgm.teams.TeamMatchModule;

public class TeamSwitchKit extends DelayedKit {

  private final TeamFactory team;

  public TeamSwitchKit(TeamFactory team) {
    this.team = team;
  }

  @Override
  public void applyDelayed(MatchPlayer player, boolean force) {
    TeamMatchModule tmm = player.getMatch().getModule(TeamMatchModule.class);
    if (tmm != null) {
      tmm.setForced(player, true);
      tmm.forceJoin(player, tmm.getTeam(team));
    }
  }
}
