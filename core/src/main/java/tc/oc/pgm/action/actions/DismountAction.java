package tc.oc.pgm.action.actions;

import org.jetbrains.annotations.Nullable;
import tc.oc.pgm.action.ActionContext;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.vehicle.VehicleMatchModule;

/**
 * Takes the player out of whatever they are riding, even if a {@code dismount-filter} would keep
 * them in.
 *
 * <pre>{@code <dismount remove="true"/>}</pre>
 *
 * <p>{@code remove}: also remove the vehicle. When left out, vehicles spawned by {@code <mount>}
 * follow its {@code remove-on-exit} and anything else is left in the world.
 */
public class DismountAction extends AbstractAction<MatchPlayer> {

  private final @Nullable Boolean remove;

  public DismountAction(@Nullable Boolean remove) {
    super(MatchPlayer.class);
    this.remove = remove;
  }

  @Override
  public void trigger(MatchPlayer player, ActionContext context) {
    VehicleMatchModule vehicles = player.getMatch().getModule(VehicleMatchModule.class);
    if (vehicles != null) vehicles.dismount(player, remove);
    else player.getBukkit().leaveVehicle();
  }
}
