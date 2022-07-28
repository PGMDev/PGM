package tc.oc.pgm.filters.modifier;

import org.jetbrains.annotations.Nullable;
import tc.oc.pgm.api.filter.Filter;
import tc.oc.pgm.api.filter.query.PartyQuery;
import tc.oc.pgm.api.filter.query.PlayerQuery;

/** Transforms a player query into a query on their team. */
public class SameTeamQueryModifier extends QueryModifier<PartyQuery, PartyQuery> {

  public SameTeamQueryModifier(Filter child) {
    super(child, PartyQuery.class);
  }

  @Nullable
  @Override
  protected PartyQuery transformQuery(PartyQuery query) {
    if (query instanceof PlayerQuery) {
      query = new tc.oc.pgm.filters.query.PartyQuery(query.getEvent(), query.getParty());
    }
    return query;
  }

  @Override
  public Class<? extends PartyQuery> queryType() {
    return PartyQuery.class;
  }
}
