package tc.oc.pgm.filters.query;

import java.util.UUID;
import javax.annotation.Nullable;
import tc.oc.pgm.match.MatchPlayer;

/** A query for a player that may or may not be online or participating in the match. */
public interface IPlayerQuery extends IPartyQuery, IEntityTypeQuery, ILocationQuery {
  UUID getPlayerId();

  /** Return the {@link MatchPlayer} for this player if they are online, otherwise null. */
  @Nullable
  MatchPlayer getPlayer();
}
