package tc.oc.pgm.util.listener;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.entity.Vehicle;
import org.bukkit.entity.minecart.ExplosiveMinecart;
import org.bukkit.event.*;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.vehicle.VehicleCreateEvent;
import org.bukkit.inventory.ItemStack;
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
  private Player lastPlacer;
  private ItemStack placingStack;
  private Location railLocation;

  private static void handleCall(Event pgmEvent, Event bukkitEvent) {
    if (bukkitEvent instanceof Cancellable) {
      ((Cancellable) pgmEvent).setCancelled(((Cancellable) bukkitEvent).isCancelled());
      Bukkit.getServer().getPluginManager().callEvent(pgmEvent);
      ((Cancellable) bukkitEvent).setCancelled(((Cancellable) pgmEvent).isCancelled());
    } else {
      Bukkit.getServer().getPluginManager().callEvent(pgmEvent);
    }
  }

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onInteraction(PlayerInteractEvent event) {
    ItemStack stack = event.getItem();
    if (stack != null && stack.getType() == Material.EXPLOSIVE_MINECART) {
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
    if (!(event instanceof Cancellable) || !((Cancellable) event).isCancelled()) {
      if (lastPlacer != null) {
        Vehicle vehicle = event.getVehicle();
        Location vehicleLocation = vehicle.getLocation();

        if (vehicle instanceof ExplosiveMinecart
            && areBlockLocationsEqual(railLocation, vehicleLocation)) {
          ItemStack itemInHand = lastPlacer.getItemInHand();

          PlayerSpawnEntityEvent pgmEvent =
              new PlayerSpawnEntityEvent(
                  lastPlacer,
                  vehicle,
                  vehicleLocation,
                  itemInHand != null ? itemInHand : placingStack);
          handleCall(pgmEvent, event);

          if (pgmEvent.isCancelled()) {
            if (!(event instanceof Cancellable)) {
              vehicle.remove();
            }
            lastPlacer.getInventory().setItemInHand(placingStack);
          }
        }
      }
    }
    lastPlacer = null;
    placingStack = null;
    railLocation = null;
  }

  private static boolean areBlockLocationsEqual(Location locA, Location locB) {
    return locA.getBlockX() == locB.getBlockX()
        && locA.getBlockY() == locB.getBlockY()
        && locA.getBlockZ() == locB.getBlockZ();
  }
}
