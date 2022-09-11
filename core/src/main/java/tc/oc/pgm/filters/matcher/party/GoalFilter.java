package tc.oc.pgm.filters.matcher.party;

import java.util.Collection;
import java.util.Collections;
import java.util.Optional;
import org.bukkit.event.Event;
import tc.oc.pgm.api.feature.FeatureReference;
import tc.oc.pgm.api.filter.query.MatchQuery;
import tc.oc.pgm.api.party.Competitor;
import tc.oc.pgm.goals.GoalDefinition;
import tc.oc.pgm.goals.events.GoalCompleteEvent;

/**
 * Matches teams/players who have completed the given objective. If a team is given, then the filter
 * matches whenever that team has completed the objective, regardless of what is passed to the
 * query. If the anyTeam flag is set, then the filter matches when any team has completed the
 * objective.
 */
public class GoalFilter implements CompetitorFilter {
  private final FeatureReference<? extends GoalDefinition> goal;

  public GoalFilter(FeatureReference<? extends GoalDefinition> goal) {
    this.goal = goal;
  }

  @Override
  public Collection<Class<? extends Event>> getRelevantEvents() {
    return Collections.singleton(GoalCompleteEvent.class);
  }

  public boolean matches(MatchQuery query, Optional<Competitor> competitor) {
    return this.goal.get().getGoal(query.getMatch()).isCompleted(competitor);
  }

  @Override
  public boolean matchesAny(MatchQuery query) {
    return matches(query, Optional.empty());
  }

  @Override
  public boolean matches(MatchQuery query, Competitor competitor) {
    return matches(query, Optional.of(competitor));
  }
}
