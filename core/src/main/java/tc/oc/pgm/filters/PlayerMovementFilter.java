package tc.oc.pgm.filters;

import tc.oc.pgm.api.filter.query.PlayerQuery;
import tc.oc.pgm.api.player.MatchPlayer;

public class PlayerMovementFilter extends ParticipantFilter {
  private final boolean sprinting;
  private final boolean sneaking;

  public PlayerMovementFilter(boolean sprinting, boolean sneaking) {
    this.sprinting = sprinting;
    this.sneaking = sneaking;
  }

  @Override
  protected QueryResponse queryPlayer(PlayerQuery query, MatchPlayer player) {
    return QueryResponse.fromBoolean(
        sprinting == player.getBukkit().isSprinting()
            && sneaking == player.getBukkit().isSneaking());
  }
}
