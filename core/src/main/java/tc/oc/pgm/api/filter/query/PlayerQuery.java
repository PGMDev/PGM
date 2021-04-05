package tc.oc.pgm.api.filter.query;

import java.util.UUID;
import javax.annotation.Nullable;
import tc.oc.pgm.api.player.MatchPlayer;

/** A query for a player that may or may not be online or participating in the match. */
public interface PlayerQuery extends PartyQuery, EntityTypeQuery, LocationQuery {
  UUID getId();

  /*TODO this is not followed in any implementations or trusted by any other code. If this is fixed change places like
  CarryingFlagFilter Line 33/34 at the time of this commit
  ParticipantFilter Line 26/27
  RegionMatchModule Line 375/376 etc...*/
  /** Return the {@link MatchPlayer} for this player if they are online, otherwise null. */
  @Nullable
  MatchPlayer getPlayer();
}
