package tc.oc.pgm.filters.query;

import static tc.oc.pgm.util.Assert.assertNotNull;

import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.party.Party;

public class PartyQuery extends Query implements tc.oc.pgm.api.filter.query.PartyQuery {

  private final Party party;

  public PartyQuery(@Nullable Event event, Party party) {
    super(event);
    this.party = assertNotNull(party);
  }

  @Override
  public Party getParty() {
    return party;
  }

  @Override
  public Match getMatch() {
    return party.getMatch();
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof PartyQuery)) return false;
    PartyQuery query = (PartyQuery) o;
    if (!party.equals(query.party)) return false;
    return true;
  }

  @Override
  public int hashCode() {
    return party.hashCode();
  }
}
