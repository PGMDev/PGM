package tc.oc.pgm.util.listener;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
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
    }
  }

  @EventHandler(ignoreCancelled = true)
  public void onVehicleCreate(VehicleCreateEvent event) {
    if (lastPlacer != null) {
      PlayerSpawnEntityEvent pgmEvent =
          new PlayerSpawnEntityEvent(
              lastPlacer,
              event.getVehicle(),
              event.getVehicle().getLocation(),
              lastPlacer.getItemInHand());
      handleCall(pgmEvent, event);

      /*
       * This block only exists to ensure consistency, as VehicleCreateEvent
       * is natively cancellable starting from 1.11.2. The TNT minecart is
       * still being used up, however.
       */
      if (pgmEvent.isCancelled()) {
        if (!(event instanceof Cancellable)) {
          event.getVehicle().remove();
        }
        lastPlacer.getInventory().setItemInHand(placingStack);
      }
    }

    lastPlacer = null;
    placingStack = null;
  }
}
