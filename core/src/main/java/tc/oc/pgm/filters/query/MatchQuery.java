package tc.oc.pgm.filters.query;

import static com.google.common.base.Preconditions.checkNotNull;

import javax.annotation.Nullable;
import org.bukkit.event.Event;
import tc.oc.pgm.api.match.Match;

public class MatchQuery extends Query implements IMatchQuery {

  private final Match match;

  public MatchQuery(@Nullable Event event, Match match) {
    super(event);
    this.match = checkNotNull(match);
  }

  @Override
  public Match getMatch() {
    return match;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof MatchQuery)) return false;
    MatchQuery query = (MatchQuery) o;
    if (!match.equals(query.match)) return false;
    return true;
  }

  @Override
  public int hashCode() {
    return match.hashCode();
  }
}
