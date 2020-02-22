package tc.oc.pgm.filters;

import com.google.common.base.Preconditions;
import javax.annotation.Nullable;
import tc.oc.pgm.api.party.Competitor;
import tc.oc.pgm.api.party.Party;
import tc.oc.pgm.features.FeatureReference;
import tc.oc.pgm.filters.query.IMatchQuery;
import tc.oc.pgm.filters.query.IPartyQuery;
import tc.oc.pgm.goals.Goal;
import tc.oc.pgm.goals.GoalDefinition;
import tc.oc.pgm.teams.TeamFactory;
import tc.oc.pgm.teams.TeamMatchModule;

/**
 * Matches teams/players who have completed the given objective. If a team is given, then the filter
 * matches whenever that team has completed the objective, regardless of what is passed to the
 * query. If the anyTeam flag is set, then the filter matches when any team has completed the
 * objective.
 */
public class GoalFilter extends TypedFilter<IMatchQuery> {
  private final FeatureReference<? extends GoalDefinition> goal;

  // Null means get the team from the query parameter
  private final @Nullable FeatureReference<TeamFactory> team;

  // True means match if the goal is complete for any team
  private final boolean anyTeam;

  public GoalFilter(
      FeatureReference<? extends GoalDefinition> goal,
      @Nullable FeatureReference<TeamFactory> team,
      boolean anyTeam) {
    Preconditions.checkArgument(
        !(anyTeam && team != null), "Cannot specify a team if anyTeam is true");
    this.goal = goal;
    this.team = team;
    this.anyTeam = anyTeam;
  }

  @Override
  public Class<? extends IMatchQuery> getQueryType() {
    return IMatchQuery.class;
  }

  @Override
  protected QueryResponse queryTyped(IMatchQuery query) {
    Goal<? extends GoalDefinition> goal = this.goal.get().getGoal(query.getMatch());
    if (goal == null) {
      return QueryResponse.ABSTAIN;
    }

    if (!goal.isShared() || this.anyTeam) {
      // Goal is shared, or team is explicitly ignored
      return QueryResponse.fromBoolean(goal.isCompleted());
    } else if (team != null) {
      // Team was explicitly specified
      return QueryResponse.fromBoolean(
          goal.isCompleted(query.getMatch().needModule(TeamMatchModule.class).getTeam(team.get())));
    } else if (query instanceof IPartyQuery) {
      final Party party = ((IPartyQuery) query).getParty();
      // Team is derived from query
      return QueryResponse.fromBoolean(
          party instanceof Competitor && goal.isCompleted((Competitor) party));
    } else {
      return QueryResponse.ABSTAIN;
    }
  }
}
