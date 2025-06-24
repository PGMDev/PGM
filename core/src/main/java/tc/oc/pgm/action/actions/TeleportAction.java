package tc.oc.pgm.action.actions;

import java.util.Optional;
import org.bukkit.Location;
import org.bukkit.event.player.PlayerTeleportEvent;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.util.math.Formula;

public class TeleportAction extends AbstractAction<MatchPlayer> {

  private final Formula<MatchPlayer> xformula;
  private final Formula<MatchPlayer> yformula;
  private final Formula<MatchPlayer> zformula;
  private final Optional<Formula<MatchPlayer>> pitchFormula;
  private final Optional<Formula<MatchPlayer>> yawFormula;

  public TeleportAction(
      Formula<MatchPlayer> xformula,
      Formula<MatchPlayer> yformula,
      Formula<MatchPlayer> zformula,
      Optional<Formula<MatchPlayer>> pitchFormula,
      Optional<Formula<MatchPlayer>> yawFormula) {
    super(MatchPlayer.class);
    this.xformula = xformula;
    this.yformula = yformula;
    this.zformula = zformula;
    this.pitchFormula = pitchFormula;
    this.yawFormula = yawFormula;
  }

  @Override
  public void trigger(MatchPlayer player) {
    Location location = player.getLocation();
    location.setX(xformula.applyAsDouble(player));
    location.setY(yformula.applyAsDouble(player));
    location.setZ(zformula.applyAsDouble(player));
    pitchFormula.ifPresent((formula) -> location.setPitch((float) formula.applyAsDouble(player)));
    yawFormula.ifPresent((formula) -> location.setYaw((float) formula.applyAsDouble(player)));
    player.getBukkit().teleport(location, PlayerTeleportEvent.TeleportCause.ENDER_PEARL);
  }
}
