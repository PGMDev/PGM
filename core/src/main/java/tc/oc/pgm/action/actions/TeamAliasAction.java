package tc.oc.pgm.action.actions;

import tc.oc.pgm.api.feature.FeatureReference;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.teams.Team;
import tc.oc.pgm.teams.TeamFactory;

public class TeamAliasAction extends AbstractAction<Match> {
  private final FeatureReference<TeamFactory> team;
  private final String alias;

  public TeamAliasAction(FeatureReference<TeamFactory> team, String alias) {
    super(Match.class);
    this.team = team;
    this.alias = alias;
  }

  @Override
  public void trigger(Match match) {
    match.getFeatureContext().get(team.get().getId(), Team.class).setName(this.alias);
  }
}
