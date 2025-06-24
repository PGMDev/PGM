package tc.oc.pgm.util.listener;

import static tc.oc.pgm.util.bukkit.BukkitUtils.parse;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.entity.Vehicle;
import org.bukkit.entity.minecart.ExplosiveMinecart;
import org.bukkit.event.Cancellable;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.vehicle.VehicleCreateEvent;
import org.bukkit.inventory.ItemStack;
import tc.oc.pgm.util.event.EventUtil;
import tc.oc.pgm.util.event.player.PlayerSpawnEntityEvent;

/**
 * Transforms vehicle creation events along with player interaction events to a synthetic
 * PlayerSpawnEntityEvent.
 *
 * <p>It works based on the fact that PlayerInteractEvent and VehicleCreateEvent are emitted in the
 * same tick.
 *
 * <p>In modern versions of Minecraft, equivalent functionality is natively implemented in Bukkit as
 * EntityPlaceEvent.
 */
public class TNTMinecartPlacementListener implements Listener {
  private static final Material TNT_MINECART =
      parse(Material::valueOf, "EXPLOSIVE_MINECART", "TNT_MINECART");
  private Player lastPlacer;
  private ItemStack placingStack;
  private Location railLocation;

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onInteraction(PlayerInteractEvent event) {
    ItemStack stack = event.getItem();
    if (stack != null
        && event.getAction() == Action.RIGHT_CLICK_BLOCK
        && stack.getType() == TNT_MINECART) {
      lastPlacer = event.getPlayer();
      placingStack = stack.clone();
      railLocation = event.getClickedBlock().getLocation();
    }
  }

  @EventHandler
  public void onVehicleCreate(VehicleCreateEvent event) {
    /*
     * Starting from 1.11.2, VehicleCreateEvent is natively cancellable.
     * Even then, we should account for this case and reset the variables
     * to ensure the tracking works reliably.
     */
    if (lastPlacer != null
        && (!(event instanceof Cancellable) || !((Cancellable) event).isCancelled())) {
      Vehicle vehicle = event.getVehicle();

      if (vehicle instanceof ExplosiveMinecart
          && isSameBlock(railLocation, vehicle.getLocation())) {
        var pgmEvent = new PlayerSpawnEntityEvent(lastPlacer, vehicle, placingStack);
        EventUtil.handleCall(pgmEvent, event);
      }
    }
    lastPlacer = null;
    placingStack = null;
    railLocation = null;
  }

  private static boolean isSameBlock(Location locA, Location locB) {
    return locA.getBlockX() == locB.getBlockX()
        && locA.getBlockY() == locB.getBlockY()
        && locA.getBlockZ() == locB.getBlockZ();
  }
}
