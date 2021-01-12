package tc.oc.pgm.api.filter.query;

import java.util.UUID;
import javax.annotation.Nullable;
import tc.oc.pgm.api.player.MatchPlayer;

/** A query for a player that may or may not be online or participating in the match. */
public interface PlayerQuery extends PartyQuery, EntityTypeQuery, LocationQuery {
  UUID getPlayerId();

  /** Return the {@link MatchPlayer} for this player if they are online, otherwise null. */
  @Nullable
  MatchPlayer getPlayer();
}
