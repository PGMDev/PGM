package tc.oc.pgm.kits;

import java.util.EnumSet;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.join.JoinRequest;
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

    EnumSet<JoinRequest.Flag> flags = EnumSet.of(JoinRequest.Flag.FORCE);
    if (showTitle) flags.add(JoinRequest.Flag.SHOW_TITLE);
    JoinRequest request = JoinRequest.of(tmm.getTeam(team), flags);
    tmm.join(player, request);
  }
}
