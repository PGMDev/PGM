package tc.oc.pgm.filters.matcher.party;

import tc.oc.pgm.api.filter.query.MatchQuery;
import tc.oc.pgm.api.filter.query.PartyQuery;
import tc.oc.pgm.api.party.Competitor;
import tc.oc.pgm.api.party.Party;
import tc.oc.pgm.filters.matcher.TypedFilter;

/**
 * A filter that can be applied to single {@link Competitor}s, or all {@link Competitor}s in the
 * match (in which case it should effectively OR all of the responses).
 *
 * <p>Any other type of {@link Party} is denied.
 */
public abstract class CompetitorFilter extends TypedFilter.Impl<PartyQuery> {

  /**
   * Does ANY {@link Competitor} match the filter?
   *
   * <p>The base method queries each competitor one by one.
   */
  public boolean matchesAny(MatchQuery query) {
    return query.getMatch().getCompetitors().stream()
        .anyMatch(competitor -> matches(query, competitor));
  }

  /** Respond to the given {@link Competitor} */
  public abstract boolean matches(MatchQuery query, Competitor competitor);

  @Override
  public final boolean matches(PartyQuery query) {
    return query.getParty() instanceof Competitor && matches(query, (Competitor) query.getParty());
  }
}
