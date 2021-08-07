package tc.oc.pgm.filters.modifier.relation;


import org.checkerframework.checker.nullness.qual.Nullable;
import tc.oc.pgm.api.filter.Filter;
import tc.oc.pgm.api.filter.query.PartyQuery;
import tc.oc.pgm.api.filter.query.PlayerQuery;
import tc.oc.pgm.api.filter.query.Query;
import tc.oc.pgm.filters.modifier.QueryModifier;

/** Transforms a player query into a query on their team. */
public class SameTeamQueryModifier extends QueryModifier<PartyQuery> {

  public SameTeamQueryModifier(Filter child) {
    super(child);
  }

  @Nullable
  @Override
  protected Query modifyQuery(PartyQuery query) {
    if (query instanceof PlayerQuery) {
      query = new tc.oc.pgm.filters.query.PartyQuery(query.getEvent(), query.getParty());
    }
    return query;
  }

  @Override
  public Class<? extends PartyQuery> getQueryType() {
    return PartyQuery.class;
  }
}
