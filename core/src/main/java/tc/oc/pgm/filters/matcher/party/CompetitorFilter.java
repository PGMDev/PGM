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
public interface CompetitorFilter extends TypedFilter<PartyQuery> {

  /**
   * Does ANY {@link Competitor} match the filter?
   *
   * <p>The base method queries each competitor one by one.
   */
  default boolean matchesAny(MatchQuery query) {
    return query.getMatch().getCompetitors().stream()
        .anyMatch(competitor -> matches(query, competitor));
  }

  /** Respond to the given {@link Competitor} */
  boolean matches(MatchQuery query, Competitor competitor);

  @Override
  default boolean matches(PartyQuery query) {
    return query.getParty() instanceof Competitor && matches(query, (Competitor) query.getParty());
  }

  @Override
  default Class<? extends PartyQuery> queryType() {
    return PartyQuery.class;
  }
}
