package tc.oc.pgm.action.actions;

import org.bukkit.util.Vector;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.util.math.Formula;

public class VelocityAction extends AbstractAction<MatchPlayer> {
  private final Formula<MatchPlayer> xformula;
  private final Formula<MatchPlayer> yformula;
  private final Formula<MatchPlayer> zformula;

  public VelocityAction(
      Formula<MatchPlayer> xformula, Formula<MatchPlayer> yformula, Formula<MatchPlayer> zformula) {
    super(MatchPlayer.class);
    this.xformula = xformula;
    this.yformula = yformula;
    this.zformula = zformula;
  }

  @Override
  public void trigger(MatchPlayer matchPlayer) {
    double x = xformula.applyAsDouble(matchPlayer);
    double y = yformula.applyAsDouble(matchPlayer);
    double z = zformula.applyAsDouble(matchPlayer);
    matchPlayer.getBukkit().setVelocity(new Vector(x, y, z));
  }
}
