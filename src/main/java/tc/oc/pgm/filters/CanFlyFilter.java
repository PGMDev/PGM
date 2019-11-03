package tc.oc.pgm.filters;

import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.filters.query.IPlayerQuery;

public class CanFlyFilter extends ParticipantFilter {

  @Override
  protected QueryResponse queryPlayer(IPlayerQuery query, MatchPlayer player) {
    return QueryResponse.fromBoolean(player.getBukkit().getAllowFlight());
  }
}
