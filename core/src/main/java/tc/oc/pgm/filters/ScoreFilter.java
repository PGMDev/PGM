package tc.oc.pgm.filters;

import com.google.common.collect.Range;
import tc.oc.pgm.api.filter.query.PartyQuery;
import tc.oc.pgm.api.party.Competitor;
import tc.oc.pgm.score.ScoreMatchModule;

/**
 * Filter which checks if a {@link tc.oc.pgm.api.party.Party}'s score is within a supplied range.
 *
 * <p>Will ABSTAIN if the score module is not loaded.
 */
public class ScoreFilter extends TypedFilter<PartyQuery> {

  private final Range<Integer> values;

  public ScoreFilter(Range<Integer> values) {
    this.values = values;
  }

  @Override
  public Class<? extends PartyQuery> getQueryType() {
    return PartyQuery.class;
  }

  @Override
  protected QueryResponse queryTyped(PartyQuery query) {
    ScoreMatchModule module = query.getMatch().getModule(ScoreMatchModule.class);
    if (module == null) return QueryResponse.ABSTAIN;

    return QueryResponse.fromBoolean(
        query.getParty() instanceof Competitor
            && values.contains((int) module.getScore((Competitor) query.getParty())));
  }
}
