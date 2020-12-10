package tc.oc.pgm.util.event;

import javax.annotation.Nullable;
import org.bukkit.Physical;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.event.Cancellable;
import org.bukkit.event.EntityAction;
import org.bukkit.event.Event;

/** An event that wraps another event. */
public abstract class GeneralizedEvent extends PreemptiveEvent implements Physical, EntityAction {

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

  // TODO: Un-implement Physical and EntityAction, since they are SportPaper only
  @Override
  public World getWorld() {
    return getCause() instanceof Physical ? ((Physical) getCause()).getWorld() : null;
  }

  @Override
  public Entity getActor() {
    return getCause() instanceof EntityAction ? ((EntityAction) getCause()).getActor() : null;
  }
}
