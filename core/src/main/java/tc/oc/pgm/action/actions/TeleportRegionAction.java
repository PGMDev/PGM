package tc.oc.pgm.action.actions;

import java.util.Optional;
import org.bukkit.Location;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.util.Vector;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.api.region.Region;
import tc.oc.pgm.util.math.Formula;

public class TeleportRegionAction extends AbstractAction<MatchPlayer> {

  private final Region.Static region;
  private final Optional<Formula<MatchPlayer>> pitchFormula;
  private final Optional<Formula<MatchPlayer>> yawFormula;

  public TeleportRegionAction(
      Region.Static region,
      Optional<Formula<MatchPlayer>> pitchFormula,
      Optional<Formula<MatchPlayer>> yawFormula) {
    super(MatchPlayer.class);
    this.region = region;
    this.pitchFormula = pitchFormula;
    this.yawFormula = yawFormula;
  }

  @Override
  public void trigger(MatchPlayer player) {
    Vector vec = region.getRandom(player.getMatch().getRandom());
    Location location = new Location(player.getWorld(), vec.getX(), vec.getY(), vec.getZ());

    pitchFormula.ifPresent(f -> location.setPitch((float) f.applyAsDouble(player)));
    yawFormula.ifPresent(f -> location.setYaw((float) f.applyAsDouble(player)));

    player.getBukkit().teleport(location, PlayerTeleportEvent.TeleportCause.ENDER_PEARL);
  }
}
