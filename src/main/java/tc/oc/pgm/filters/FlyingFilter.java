package tc.oc.pgm.filters;

import tc.oc.pgm.filters.query.IPlayerQuery;
import tc.oc.pgm.match.MatchPlayer;

public class FlyingFilter extends ParticipantFilter {

  @Override
  protected QueryResponse queryPlayer(IPlayerQuery query, MatchPlayer player) {
    return QueryResponse.fromBoolean(player.getBukkit().isFlying());
  }
}
