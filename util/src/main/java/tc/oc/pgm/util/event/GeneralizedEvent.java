package tc.oc.pgm.util.event;

import javax.annotation.Nullable;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.EventException;
import org.bukkit.event.entity.EntityEvent;
import org.bukkit.event.player.PlayerEvent;
import org.bukkit.event.world.WorldEvent;

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
    if (!(getCause() instanceof WorldEvent))
      throw new EventException("Event has no associated world");
    return ((WorldEvent) getCause()).getWorld();
  }

  public @Nullable Entity getActor() {
    return getActorIfPresent(getCause());
  }

  public static @Nullable Entity getActorIfPresent(Event event) {
    if (event == null) return null;
    return event instanceof EntityEvent
        ? ((EntityEvent) event).getEntity()
        : event instanceof PlayerEvent ? ((PlayerEvent) event).getPlayer() : null;
  }
}
