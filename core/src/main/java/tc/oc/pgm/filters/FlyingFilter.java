package tc.oc.pgm.filters;

import tc.oc.pgm.api.filter.query.PlayerQuery;
import tc.oc.pgm.api.player.MatchPlayer;

public class FlyingFilter extends ParticipantFilter {

  @Override
  protected QueryResponse queryPlayer(PlayerQuery query, MatchPlayer player) {
    return QueryResponse.fromBoolean(player.getBukkit().isFlying());
  }
}
