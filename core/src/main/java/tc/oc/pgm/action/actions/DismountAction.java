package tc.oc.pgm.action.actions;

import org.jetbrains.annotations.Nullable;
import tc.oc.pgm.action.ActionContext;
import tc.oc.pgm.api.filter.Filter;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.vehicle.VehicleMatchModule;

/**
 * Takes the player out of whatever they are riding, even if a {@code dismount-filter} would keep
 * them in.
 *
 * <pre>{@code <dismount remove-filter="always"/>}</pre>
 *
 * <p>{@code remove-filter}: queried against the player once they are out, to decide whether the
 * vehicle is removed too. When left out, entities spawned by {@code <mount>} use its own {@code
 * remove-filter} and anything else is left in the world.
 */
public class DismountAction extends AbstractAction<MatchPlayer> {

  private final @Nullable Filter removeFilter;

  public DismountAction(@Nullable Filter removeFilter) {
    super(MatchPlayer.class);
    this.removeFilter = removeFilter;
  }

  @Override
  public void trigger(MatchPlayer player, ActionContext context) {
    VehicleMatchModule vehicles = player.getMatch().getModule(VehicleMatchModule.class);
    if (vehicles != null) vehicles.dismount(player, removeFilter);
    else player.getBukkit().leaveVehicle();
  }
}
