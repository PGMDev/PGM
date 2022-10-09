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
import org.jetbrains.annotations.Nullable;

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

    if (event instanceof EntityEvent) return ((EntityEvent) event).getEntity();
    if (event instanceof PlayerEvent) return ((PlayerEvent) event).getPlayer();
    if (event instanceof BlockEvent) {
      if (event instanceof BlockPlaceEvent) return ((BlockPlaceEvent) event).getPlayer();
      if (event instanceof BlockBreakEvent) return ((BlockBreakEvent) event).getPlayer();
      if (event instanceof BlockDamageEvent) return ((BlockDamageEvent) event).getPlayer();
      if (event instanceof FurnaceExtractEvent) return ((FurnaceExtractEvent) event).getPlayer();
      if (event instanceof SignChangeEvent) return ((SignChangeEvent) event).getPlayer();
    }
    if (event instanceof VehicleEvent) {
      if (event instanceof VehicleExitEvent) return ((VehicleExitEvent) event).getExited();
      if (event instanceof VehicleDamageEvent) return ((VehicleDamageEvent) event).getAttacker();
      if (event instanceof VehicleDestroyEvent) return ((VehicleDestroyEvent) event).getAttacker();
      if (event instanceof VehicleEnterEvent) return ((VehicleEnterEvent) event).getEntered();
      if (event instanceof VehicleEntityCollisionEvent)
        return ((VehicleEntityCollisionEvent) event).getEntity();
      return ((VehicleEvent) event).getVehicle();
    }
    if (event instanceof GeneralizedEvent) return ((GeneralizedEvent) event).getActor();

    return null;
  }

  /**
   * Tries to extract a {@link World} from a event.
   *
   * @param event The event to look for a {@link World} in
   */
  public static @Nullable World getWorldIfPresent(Event event) {
    if (event instanceof WorldEvent) return ((WorldEvent) event).getWorld();
    if (event instanceof PlayerEvent) return ((PlayerEvent) event).getPlayer().getWorld();
    if (event instanceof EntityEvent) return ((EntityEvent) event).getEntity().getWorld();
    if (event instanceof BlockEvent) return ((BlockEvent) event).getBlock().getWorld();
    if (event instanceof VehicleEvent) return ((VehicleEvent) event).getVehicle().getWorld();
    if (event instanceof WeatherEvent) return ((WeatherEvent) event).getWorld();

    return null;
  }
}
