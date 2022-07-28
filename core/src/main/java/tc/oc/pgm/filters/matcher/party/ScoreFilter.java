package tc.oc.pgm.filters.matcher.party;

import com.google.common.collect.Range;
import java.util.Collection;
import java.util.Collections;
import org.bukkit.event.Event;
import tc.oc.pgm.api.filter.query.MatchQuery;
import tc.oc.pgm.api.filter.query.PartyQuery;
import tc.oc.pgm.api.party.Competitor;
import tc.oc.pgm.api.party.event.CompetitorScoreChangeEvent;
import tc.oc.pgm.score.ScoreMatchModule;

/** Match whether a {@link Competitor}'s score is within a range. */
public class ScoreFilter extends CompetitorFilter {

  private final Range<Integer> values;

  public ScoreFilter(Range<Integer> values) {
    this.values = values;
  }

  @Override
  public Collection<Class<? extends Event>> getRelevantEvents() {
    return Collections.singleton(CompetitorScoreChangeEvent.class);
  }

  @Override
  public Class<? extends PartyQuery> queryType() {
    return PartyQuery.class;
  }

  @Override
  public boolean matches(MatchQuery query, Competitor competitor) {
    return query
        .moduleOptional(ScoreMatchModule.class)
        .filter(smm -> values.contains((int) smm.getScore(competitor)))
        .isPresent();
  }
}
