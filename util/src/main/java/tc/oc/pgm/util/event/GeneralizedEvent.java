package tc.oc.pgm.util.event;

import java.lang.reflect.Method;
import javax.annotation.Nullable;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.EventException;
import org.bukkit.event.entity.EntityEvent;
import org.bukkit.event.player.PlayerEvent;

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

    Entity entity =
        event instanceof EntityEvent
            ? ((EntityEvent) event).getEntity()
            : event instanceof PlayerEvent ? ((PlayerEvent) event).getPlayer() : null;

    if (entity == null) {
      // Look for any hidden actors
      for (Method method : event.getClass().getMethods()) {
        if (method.getName().equals("getActor"))
          try {
            entity = (Entity) method.invoke(event);
          } catch (Throwable ignored) {
          }
      }
    }

    return entity;
  }

  /**
   * Tries to extract a {@link World} from a event.
   *
   * @param event The event to look for a {@link World} in
   */
  public static @Nullable World getWorldIfPresent(Event event) {

    try { // WorldEvent, WeatherEvent
      for (Method method : event.getClass().getMethods()) {
        if (method.getName().equals("getWorld")) {
          return (World) method.invoke(event);
        }
      }
    } catch (Throwable ignored) {
      try {
        // Try to find a world one level down
        Method[] methods = event.getClass().getMethods();
        for (Method method : methods) {
          if (method.getName().startsWith("get") && method.getParameterCount() == 0) {
            try {
              // BlockEvent, VehicleEvent, EntityEvent, PlayerEvent
              Object object = method.invoke(event);
              for (Method method0 : object.getClass().getMethods()) {
                if (method0.getName().equals("getWorld")) return (World) method0.invoke(object);
              }

            } catch (Throwable ignored0) {
            }
          }
        }
      } catch (Throwable ignored1) {
      }
    }

    return null; // No world found
  }
}
