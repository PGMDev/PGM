package tc.oc.pgm.filters;

import tc.oc.pgm.api.filter.query.PlayerQuery;
import tc.oc.pgm.api.player.MatchPlayer;

public class GroundedFilter extends ParticipantFilter {

  public static final GroundedFilter INSTANCE = new GroundedFilter();

  @Override
  protected QueryResponse queryPlayer(PlayerQuery query, MatchPlayer player) {
    return QueryResponse.fromBoolean(player.getBukkit().isOnGround());
  }
}
