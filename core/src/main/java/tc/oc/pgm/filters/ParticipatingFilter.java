package tc.oc.pgm.filters;

import tc.oc.pgm.api.filter.query.PartyQuery;
import tc.oc.pgm.api.party.Party;

public class ParticipatingFilter extends TypedFilter<PartyQuery> {

  public static final ParticipatingFilter PARTICIPATING = new ParticipatingFilter(true);
  public static final ParticipatingFilter OBSERVING = new ParticipatingFilter(false);

  private final boolean participating;

  public ParticipatingFilter(boolean participating) {
    this.participating = participating;
  }

  @Override
  public Class<? extends PartyQuery> getQueryType() {
    return PartyQuery.class;
  }

  @Override
  protected QueryResponse queryTyped(PartyQuery query) {
    final Party party = query.getParty();
    return QueryResponse.fromBoolean(party.isParticipating() == participating);
  }
}
