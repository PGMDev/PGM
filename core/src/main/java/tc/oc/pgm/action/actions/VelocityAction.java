package tc.oc.pgm.action.actions;

import org.bukkit.util.Vector;
import tc.oc.pgm.action.ActionContext;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.util.VectorUtils;
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
  public void trigger(MatchPlayer matchPlayer, ActionContext context) {
    double x = xformula.apply(matchPlayer, context.locals());
    double y = yformula.apply(matchPlayer, context.locals());
    double z = zformula.apply(matchPlayer, context.locals());
    matchPlayer.getBukkit().setVelocity(VectorUtils.clampVelocityVector(new Vector(x, y, z)));
  }
}
