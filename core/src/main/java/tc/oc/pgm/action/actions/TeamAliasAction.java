package tc.oc.pgm.action.actions;

import tc.oc.pgm.api.feature.FeatureReference;
import tc.oc.pgm.filters.Filterable;
import tc.oc.pgm.match.MatchImpl;
import tc.oc.pgm.match.PartyImpl;
import tc.oc.pgm.teams.Team;
import tc.oc.pgm.teams.TeamFactory;

public class TeamAliasAction<T extends Filterable<?>> extends AbstractAction<T> {
  protected final String alias;

  public TeamAliasAction(Class<T> scope, String alias) {
    super(scope);
    this.alias = alias;
  }

  @Override
  public void trigger(T scope) {
    if (scope instanceof PartyImpl) {
      ((PartyImpl) scope).getParty().setName(this.alias);
    }
  }
  ;

  public static class WithTeam<T extends Filterable<?>> extends TeamAliasAction<T> {
    private final FeatureReference<TeamFactory> team;

    public WithTeam(Class<T> scope, String alias, FeatureReference<TeamFactory> team) {
      super(scope, alias);
      this.team = team;
    }

    @Override
    public void trigger(T scope) {
      if (scope instanceof MatchImpl) {
        ((MatchImpl) scope)
            .getFeatureContext()
            .get(team.get().getId(), Team.class)
            .setName(this.alias);
      }
    }
    ;
  }
}
