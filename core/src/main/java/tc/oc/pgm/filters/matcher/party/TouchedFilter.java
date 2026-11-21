package tc.oc.pgm.filters.matcher.party;

import com.google.common.collect.ImmutableList;
import java.util.Collection;
import org.bukkit.event.Event;
import tc.oc.pgm.api.feature.FeatureReference;
import tc.oc.pgm.api.filter.query.MatchQuery;
import tc.oc.pgm.api.filter.query.PartyQuery;
import tc.oc.pgm.api.filter.query.PlayerQuery;
import tc.oc.pgm.api.party.Competitor;
import tc.oc.pgm.api.party.event.CompetitorRemoveEvent;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.flag.event.FlagStateChangeEvent;
import tc.oc.pgm.goals.ProximityGoalDefinition;
import tc.oc.pgm.goals.TouchableGoal;
import tc.oc.pgm.goals.events.GoalCompleteEvent;
import tc.oc.pgm.goals.events.GoalTouchEvent;

public class TouchedFilter implements CompetitorFilter {
  private final FeatureReference<? extends ProximityGoalDefinition> goal;

  public TouchedFilter(FeatureReference<? extends ProximityGoalDefinition> goal) {
    this.goal = goal;
  }

  @Override
  public Collection<Class<? extends Event>> getRelevantEvents() {
    return ImmutableList.of(
        GoalTouchEvent.class,
        GoalCompleteEvent.class,
        FlagStateChangeEvent.class,
        CompetitorRemoveEvent.class);
  }

  private TouchableGoal<?> getGoal(MatchQuery query) {
    return (TouchableGoal<?>) goal.get().getGoal(query.getMatch());
  }

  @Override
  public boolean matches(MatchQuery query, Competitor competitor) {
    return getGoal(query).hasTouched(competitor);
  }

  @Override
  public boolean matches(PartyQuery query) {
    if (query instanceof PlayerQuery playerQuery) {
      MatchPlayer player = playerQuery.getPlayer();
      return player != null && getGoal(query).hasTouched(player);
    }
    return CompetitorFilter.super.matches(query);
  }
}
