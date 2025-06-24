package tc.oc.pgm.filters.matcher.player;

import tc.oc.pgm.api.filter.query.PlayerQuery;
import tc.oc.pgm.api.player.MatchPlayer;

public class CanFlyFilter extends ParticipantFilter {
  public static final CanFlyFilter INSTANCE = new CanFlyFilter();

  @Override
  protected boolean matches(PlayerQuery query, MatchPlayer player) {
    return player.getBukkit().getAllowFlight();
  }
}
