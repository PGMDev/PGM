package tc.oc.pgm.util.event;

import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.EventException;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockDamageEvent;
import org.bukkit.event.block.BlockEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.block.SignChangeEvent;
import org.bukkit.event.entity.EntityEvent;
import org.bukkit.event.inventory.FurnaceExtractEvent;
import org.bukkit.event.player.PlayerEvent;
import org.bukkit.event.vehicle.VehicleDamageEvent;
import org.bukkit.event.vehicle.VehicleDestroyEvent;
import org.bukkit.event.vehicle.VehicleEnterEvent;
import org.bukkit.event.vehicle.VehicleEntityCollisionEvent;
import org.bukkit.event.vehicle.VehicleEvent;
import org.bukkit.event.vehicle.VehicleExitEvent;
import org.bukkit.event.weather.WeatherEvent;
import org.bukkit.event.world.WorldEvent;
import org.jspecify.annotations.Nullable;

/** An event that wraps another event. */
public abstract class GeneralizedEvent extends PreemptiveEvent {

  private final @Nullable Event cause;
  private boolean propagate;

  protected GeneralizedEvent(final @Nullable Event cause) {
    super();
    this.cause = cause;
    this.propagate = true;
  }

  /**
   * Gets the event cause.
   *
   * @return an event
   */
  @Nullable
  public Event getCause() {
    return this.cause;
  }

  /**
   * Set whether cancelling this event, will also cancel the cause.
   *
   * @param propagate if the event should propagate cancellations
   */
  public void setPropagate(final boolean propagate) {
    this.propagate = propagate;
  }

  @Override
  public void setCancelled(final boolean cancel) {
    super.setCancelled(cancel);

    if (this.propagate && this.cause instanceof Cancellable) {
      ((Cancellable) this.cause).setCancelled(cancel);
    }
  }

  public @Nullable World getWorld() throws EventException {
    if (getCause() == null) return null;

    World world = getWorldIfPresent(getCause());
    if (world != null) return world;

    throw new EventException(getCause().getEventName() + " has no associated world");
  }

  public @Nullable Entity getActor() {
    return getActorIfPresent(getCause());
  }

  public static @Nullable Entity getActorIfPresent(Event event) {
    if (event == null) return null;

    return switch (event) {
      case EntityEvent entityEvent -> entityEvent.getEntity();
      case PlayerEvent playerEvent -> playerEvent.getPlayer();
      case BlockEvent blockEvent ->
        switch (blockEvent) {
          case BlockPlaceEvent blockPlaceEvent -> blockPlaceEvent.getPlayer();
          case BlockBreakEvent blockBreakEvent -> blockBreakEvent.getPlayer();
          case BlockDamageEvent blockDamageEvent -> blockDamageEvent.getPlayer();
          case FurnaceExtractEvent furnaceExtractEvent -> furnaceExtractEvent.getPlayer();
          case SignChangeEvent signChangeEvent -> signChangeEvent.getPlayer();
          default -> null;
        };
      case VehicleEvent vehicleEvent ->
        switch (vehicleEvent) {
          case VehicleExitEvent vehicleExitEvent -> vehicleExitEvent.getExited();
          case VehicleDamageEvent vehicleDamageEvent -> vehicleDamageEvent.getAttacker();
          case VehicleDestroyEvent vehicleDestroyEvent -> vehicleDestroyEvent.getAttacker();
          case VehicleEnterEvent vehicleEnterEvent -> vehicleEnterEvent.getEntered();
          case VehicleEntityCollisionEvent vehicleEntityCollisionEvent ->
            vehicleEntityCollisionEvent.getEntity();
          default -> vehicleEvent.getVehicle();
        };
      case GeneralizedEvent generalizedEvent -> generalizedEvent.getActor();
      default -> null;
    };
  }

  /**
   * Tries to extract a {@link World} from an event.
   *
   * @param event The event to look for a {@link World} in
   */
  public static @Nullable World getWorldIfPresent(Event event) {
    return switch (event) {
      case WorldEvent worldEvent -> worldEvent.getWorld();
      case PlayerEvent playerEvent -> playerEvent.getPlayer().getWorld();
      case EntityEvent entityEvent -> entityEvent.getEntity().getWorld();
      case BlockEvent blockEvent -> blockEvent.getBlock().getWorld();
      case VehicleEvent vehicleEvent -> vehicleEvent.getVehicle().getWorld();
      case WeatherEvent weatherEvent -> weatherEvent.getWorld();
      default -> null;
    };
  }
}
