package tc.oc.pgm.filters;

import static com.google.common.base.Preconditions.checkNotNull;

import tc.oc.pgm.api.filter.Filter;
import tc.oc.pgm.api.filter.query.PartyQuery;
import tc.oc.pgm.api.filter.query.PlayerQuery;

/** Transforms a player query into a query on their team. */
public class SameTeamFilter extends TypedFilter<PartyQuery> {

  private final Filter child;

  public SameTeamFilter(Filter child) {
    checkNotNull(child);
    this.child = child;
  }

  @Override
  public Class<? extends PartyQuery> getQueryType() {
    return PartyQuery.class;
  }

  @Override
  protected QueryResponse queryTyped(PartyQuery query) {
    if (query instanceof PlayerQuery) {
      query = new tc.oc.pgm.filters.query.PartyQuery(query.getEvent(), query.getParty());
    }
    return child.query(query);
  }
}
