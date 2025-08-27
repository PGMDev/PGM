package tc.oc.pgm.action.actions;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.party.Party;
import tc.oc.pgm.teams.Team;

public class TeamAliasAction extends AbstractAction<Match> {
  private final String teamId;
  private final String alias;

  public TeamAliasAction(String teamId, String alias) {
    super(Match.class);
    this.teamId = teamId;
    this.alias = alias;
  }

  @Override
  public void trigger(Match match) {
    List<Party> teamsList = new ArrayList<>(match.getParties());

    for (Party team : teamsList) {
      if (team instanceof Team) {
        String teamId = ((Team) team).getId();
        if (Objects.equals(teamId, this.teamId)) {
          team.setName(this.alias);
        }
      }
    }
  }
}
