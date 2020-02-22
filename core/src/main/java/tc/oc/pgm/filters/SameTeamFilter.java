package tc.oc.pgm.filters;

import static com.google.common.base.Preconditions.checkNotNull;

import tc.oc.pgm.filters.query.IPartyQuery;
import tc.oc.pgm.filters.query.IPlayerQuery;
import tc.oc.pgm.filters.query.PartyQuery;

/** Transforms a player query into a query on their team. */
public class SameTeamFilter extends TypedFilter<IPartyQuery> {

  private final Filter child;

  public SameTeamFilter(Filter child) {
    checkNotNull(child);
    this.child = child;
  }

  @Override
  public Class<? extends IPartyQuery> getQueryType() {
    return IPartyQuery.class;
  }

  @Override
  protected QueryResponse queryTyped(IPartyQuery query) {
    if (query instanceof IPlayerQuery) {
      query = new PartyQuery(query.getEvent(), query.getParty());
    }
    return child.query(query);
  }
}
