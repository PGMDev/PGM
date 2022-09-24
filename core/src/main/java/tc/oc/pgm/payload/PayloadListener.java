package tc.oc.pgm.payload;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.vehicle.VehicleDamageEvent;
import org.bukkit.event.vehicle.VehicleDestroyEvent;
import org.bukkit.event.vehicle.VehicleEnterEvent;

public class PayloadListener implements Listener {

  @EventHandler
  public void onVehicleDamage(final VehicleDamageEvent event) {
    if (Payload.isPayload(event.getVehicle())) event.setCancelled(true);
  }

  @EventHandler
  public void onVehicleEnter(final VehicleEnterEvent event) {
    if (Payload.isPayload(event.getVehicle())) event.setCancelled(true);
  }

  @EventHandler
  public void onVehicleDestroy(final VehicleDestroyEvent event) {
    if (Payload.isPayload(event.getVehicle())) event.setCancelled(true);
  }
}
