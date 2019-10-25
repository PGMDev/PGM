package tc.oc.pgm.filters;

import tc.oc.pgm.features.FeatureReference;
import tc.oc.pgm.filters.query.IPartyQuery;
import tc.oc.pgm.match.Party;
import tc.oc.pgm.teams.Team;
import tc.oc.pgm.teams.TeamFactory;

/** Match the given team, or a player on that team */
public class TeamFilter extends TypedFilter<IPartyQuery> {
  protected final FeatureReference<TeamFactory> team;

  public TeamFilter(FeatureReference<TeamFactory> team) {
    this.team = team;
  }

  @Override
  public Class<? extends IPartyQuery> getQueryType() {
    return IPartyQuery.class;
  }

  @Override
  protected QueryResponse queryTyped(IPartyQuery query) {
    final Party party = query.getParty();
    return QueryResponse.fromBoolean(
        party instanceof Team && ((Team) party).isInstance(team.get()));
  }

  @Override
  public String toString() {
    return getClass().getSimpleName() + "{team=" + this.team + "}";
  }
}
