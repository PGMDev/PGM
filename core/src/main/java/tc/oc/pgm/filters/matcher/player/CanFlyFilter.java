package tc.oc.pgm.filters.matcher.player;

import tc.oc.pgm.api.filter.query.PlayerQuery;
import tc.oc.pgm.api.player.MatchPlayer;

public class CanFlyFilter extends ParticipantFilter {

  @Override
  protected boolean matches(PlayerQuery query, MatchPlayer player) {
    return player.getBukkit().getAllowFlight();
  }
}
