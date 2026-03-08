package tc.oc.pgm.action.actions;

import org.bukkit.util.Vector;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.util.VectorUtils;
import tc.oc.pgm.util.math.Formula;

public class VelocityAction extends AbstractAction<MatchPlayer> {
  private final Formula<MatchPlayer> xformula;
  private final Formula<MatchPlayer> yformula;
  private final Formula<MatchPlayer> zformula;
  private final boolean clampValues;

  public VelocityAction(
      Formula<MatchPlayer> xformula,
      Formula<MatchPlayer> yformula,
      Formula<MatchPlayer> zformula,
      boolean clampValues) {
    super(MatchPlayer.class);
    this.xformula = xformula;
    this.yformula = yformula;
    this.zformula = zformula;
    this.clampValues = clampValues;
  }

  @Override
  public void trigger(MatchPlayer matchPlayer) {
    double x = xformula.applyAsDouble(matchPlayer);
    double y = yformula.applyAsDouble(matchPlayer);
    double z = zformula.applyAsDouble(matchPlayer);
    Vector vec = new Vector(x, y, z);
    if (clampValues) vec = VectorUtils.clampVelocityVector(vec);
    matchPlayer.getBukkit().setVelocity(vec);
  }
}
