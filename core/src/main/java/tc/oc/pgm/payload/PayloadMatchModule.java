package tc.oc.pgm.payload;

import java.util.List;
import org.bukkit.entity.Minecart;
import org.bukkit.entity.Vehicle;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.vehicle.VehicleDamageEvent;
import org.bukkit.event.vehicle.VehicleDestroyEvent;
import org.bukkit.event.vehicle.VehicleEnterEvent;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.match.MatchModule;
import tc.oc.pgm.api.match.MatchScope;
import tc.oc.pgm.events.ListenerScope;

@ListenerScope(MatchScope.LOADED)
public class PayloadMatchModule implements MatchModule, Listener {

  private final List<Payload> payloads;
  private final Match match;

  PayloadMatchModule(Match match, List<Payload> payloads) {
    match.addTickable(new PayloadTickTask(payloads), MatchScope.RUNNING);
    this.payloads = payloads;
    this.match = match;
  }

  private boolean isAnyPayload(Vehicle vehicle) {
    if (vehicle instanceof Minecart)
      return payloads.stream().anyMatch(p -> p.isPayload((Minecart) vehicle));
    return false;
  }

  // Since 1.8 spigot does not have #setInvulnerable these methods protect the payload entities
  // instead
  @EventHandler
  public void onVehicleDamage(final VehicleDamageEvent event) {
    if (isAnyPayload(event.getVehicle())) event.setCancelled(true);
  }

  @EventHandler
  public void onVehicleEnter(final VehicleEnterEvent event) {
    if (isAnyPayload(event.getVehicle())) event.setCancelled(true);
  }

  @EventHandler
  public void onVehicleDestroy(final VehicleDestroyEvent event) {
    if (isAnyPayload(event.getVehicle())) event.setCancelled(true);
  }
}
