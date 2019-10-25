package tc.oc.pgm.filters;

import tc.oc.pgm.filters.query.IPlayerQuery;
import tc.oc.pgm.match.MatchPlayer;

public class PlayerMovementFilter extends ParticipantFilter {
  private final boolean sprinting;
  private final boolean sneaking;

  public PlayerMovementFilter(boolean sprinting, boolean sneaking) {
    this.sprinting = sprinting;
    this.sneaking = sneaking;
  }

  @Override
  protected QueryResponse queryPlayer(IPlayerQuery query, MatchPlayer player) {
    return QueryResponse.fromBoolean(
        sprinting == player.getBukkit().isSprinting()
            && sneaking == player.getBukkit().isSneaking());
  }
}
