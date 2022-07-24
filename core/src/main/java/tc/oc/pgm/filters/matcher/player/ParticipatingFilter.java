package tc.oc.pgm.filters.matcher.player;

import tc.oc.pgm.api.filter.query.PartyQuery;
import tc.oc.pgm.api.party.Party;
import tc.oc.pgm.filters.matcher.TypedFilter;

public class ParticipatingFilter extends TypedFilter.Impl<PartyQuery> {

  public static final ParticipatingFilter PARTICIPATING = new ParticipatingFilter(true);
  public static final ParticipatingFilter OBSERVING = new ParticipatingFilter(false);

  private final boolean participating;

  public ParticipatingFilter(boolean participating) {
    this.participating = participating;
  }

  @Override
  public Class<? extends PartyQuery> queryType() {
    return PartyQuery.class;
  }

  @Override
  public boolean matches(PartyQuery query) {
    final Party party = query.getParty();
    return party.isParticipating() == participating;
  }
}
