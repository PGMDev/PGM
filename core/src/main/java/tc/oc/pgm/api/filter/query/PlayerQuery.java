package tc.oc.pgm.api.filter.query;

import java.util.UUID;
import org.jetbrains.annotations.Nullable;
import tc.oc.pgm.api.player.MatchPlayer;

/** A query for a player that may or may not be online or participating in the match. */
public interface PlayerQuery extends PartyQuery, EntityTypeQuery, LocationQuery, InventoryQuery {
  UUID getId();

  /**
   * Return the {@link MatchPlayer} for this player if available (ie: online), otherwise null. <br>
   * Note: the player won't be returned if the query is PlayerState based and the player changed
   * team, as the state is no longer the same, and allowing the newly-teamed player to be filtered
   * would be faulty.
   */
  @Nullable
  MatchPlayer getPlayer();
}
