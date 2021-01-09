package tc.oc.pgm.api.filter.query;

import java.util.UUID;
import javax.annotation.Nullable;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.filters.Filterable;

/** A query for a player that may or may not be online or participating in the match. */
public interface PlayerQuery
    extends PartyQuery,
        EntityTypeQuery,
        LocationQuery,
        Filterable<tc.oc.pgm.filters.query.PlayerQuery> {
  UUID getPlayerId();

  /** Return the {@link MatchPlayer} for this player if they are online, otherwise null. */
  @Nullable
  MatchPlayer getPlayer();
}
