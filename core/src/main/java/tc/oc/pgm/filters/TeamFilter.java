package tc.oc.pgm.filters;

import tc.oc.pgm.api.feature.FeatureReference;
import tc.oc.pgm.api.filter.query.PartyQuery;
import tc.oc.pgm.api.party.Party;
import tc.oc.pgm.teams.Team;
import tc.oc.pgm.teams.TeamFactory;

/** Match the given team, or a player on that team */
public class TeamFilter extends TypedFilter<PartyQuery> {
  protected final FeatureReference<TeamFactory> team;

  public TeamFilter(FeatureReference<TeamFactory> team) {
    this.team = team;
  }

  @Override
  public Class<? extends PartyQuery> getQueryType() {
    return PartyQuery.class;
  }

  @Override
  protected QueryResponse queryTyped(PartyQuery query) {
    final Party party = query.getParty();
    return QueryResponse.fromBoolean(
        party instanceof Team && ((Team) party).isInstance(team.get()));
  }

  @Override
  public String toString() {
    return getClass().getSimpleName() + "{team=" + this.team + "}";
  }
}
