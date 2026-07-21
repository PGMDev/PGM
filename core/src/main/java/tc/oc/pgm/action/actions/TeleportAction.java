package tc.oc.pgm.action.actions;

import java.util.Optional;
import org.bukkit.event.player.PlayerTeleportEvent;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.api.region.Region;
import tc.oc.pgm.util.math.Formula;

public class TeleportAction extends AbstractAction<MatchPlayer> {

  private final Optional<Region> region;
  private final Optional<Formula<MatchPlayer>> xformula;
  private final Optional<Formula<MatchPlayer>> yformula;
  private final Optional<Formula<MatchPlayer>> zformula;
  private final Optional<Formula<MatchPlayer>> pitchFormula;
  private final Optional<Formula<MatchPlayer>> yawFormula;

  public TeleportAction(
      Optional<Region> region,
      Optional<Formula<MatchPlayer>> xformula,
      Optional<Formula<MatchPlayer>> yformula,
      Optional<Formula<MatchPlayer>> zformula,
      Optional<Formula<MatchPlayer>> pitchFormula,
      Optional<Formula<MatchPlayer>> yawFormula) {
    super(MatchPlayer.class);
    this.region = region;
    this.xformula = xformula;
    this.yformula = yformula;
    this.zformula = zformula;
    this.pitchFormula = pitchFormula;
    this.yawFormula = yawFormula;
  }

  @Override
  public void trigger(MatchPlayer player) {
    var location =
        this.region.map(r -> r.getRandomLoc(player.getMatch())).orElseGet(player::getLocation);

    xformula.ifPresent(f -> location.setX(f.applyAsDouble(player)));
    yformula.ifPresent(f -> location.setY(f.applyAsDouble(player)));
    zformula.ifPresent(f -> location.setZ(f.applyAsDouble(player)));

    pitchFormula.ifPresent(f -> location.setPitch((float) f.applyAsDouble(player)));
    yawFormula.ifPresent(f -> location.setYaw((float) f.applyAsDouble(player)));

    player.getBukkit().teleport(location, PlayerTeleportEvent.TeleportCause.ENDER_PEARL);
  }
}
