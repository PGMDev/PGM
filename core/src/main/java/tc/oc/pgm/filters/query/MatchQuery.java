package tc.oc.pgm.filters.query;

import static tc.oc.pgm.util.Assert.assertNotNull;

import org.bukkit.event.Event;
import org.jspecify.annotations.Nullable;
import tc.oc.pgm.api.match.Match;

public class MatchQuery extends Query implements tc.oc.pgm.api.filter.query.MatchQuery {

  private final Match match;

  public MatchQuery(@Nullable Event event, Match match) {
    super(event);
    this.match = assertNotNull(match);
  }

  @Override
  public Match getMatch() {
    return match;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof MatchQuery query)) return false;
    return match.equals(query.match);
  }

  @Override
  public int hashCode() {
    return match.hashCode();
  }
}
