package tc.oc.pgm.filters.matcher.party;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterators;
import com.google.common.collect.Range;
import java.util.Collection;
import org.bukkit.event.Event;
import tc.oc.pgm.api.filter.query.MatchQuery;
import tc.oc.pgm.api.filter.query.PartyQuery;
import tc.oc.pgm.api.party.Competitor;
import tc.oc.pgm.api.party.event.CompetitorScoreChangeEvent;
import tc.oc.pgm.blitz.BlitzPlayerEliminatedEvent;
import tc.oc.pgm.goals.events.GoalCompleteEvent;
import tc.oc.pgm.goals.events.GoalProximityChangeEvent;
import tc.oc.pgm.goals.events.GoalTouchEvent;

/** Match whether a {@link Competitor}'s score is within a range. */
public class RankFilter extends CompetitorFilter {

  private final Range<Integer> positions;

  public RankFilter(Range<Integer> positions) {
    this.positions = positions;
  }

  @Override
  public Collection<Class<? extends Event>> getRelevantEvents() {
    return ImmutableList.of(
        CompetitorScoreChangeEvent.class,
        GoalCompleteEvent.class,
        GoalTouchEvent.class,
        GoalProximityChangeEvent.class,
        BlitzPlayerEliminatedEvent.class);
  }

  @Override
  public Class<? extends PartyQuery> queryType() {
    return PartyQuery.class;
  }

  @Override
  public boolean matches(MatchQuery query, Competitor competitor) {
    return positions.contains(
        Iterators.indexOf(
            query.getMatch().getSortedCompetitors().iterator(), c -> c == competitor));
  }
}
