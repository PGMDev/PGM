package tc.oc.pgm.filters.matcher.player;

import com.google.common.collect.ImmutableList;
import java.util.Collection;
import org.bukkit.event.Event;
import tc.oc.pgm.api.filter.query.PartyQuery;
import tc.oc.pgm.api.match.event.MatchPhaseChangeEvent;
import tc.oc.pgm.api.party.Party;
import tc.oc.pgm.events.PlayerPartyChangeEvent;
import tc.oc.pgm.filters.matcher.TypedFilter;

public class ParticipatingFilter extends TypedFilter.Impl<PartyQuery> {

  public static final ParticipatingFilter PARTICIPATING = new ParticipatingFilter(true);
  public static final ParticipatingFilter OBSERVING = new ParticipatingFilter(false);

  private final boolean participating;

  public ParticipatingFilter(boolean participating) {
    this.participating = participating;
  }

  @Override
  public Collection<Class<? extends Event>> getRelevantEvents() {
    return ImmutableList.of(MatchPhaseChangeEvent.class, PlayerPartyChangeEvent.class);
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
