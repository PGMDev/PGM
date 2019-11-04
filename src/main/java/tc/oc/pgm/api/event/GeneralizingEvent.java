package tc.oc.pgm.api.event;

import javax.annotation.Nullable;
import org.bukkit.Physical;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.event.Cancellable;
import org.bukkit.event.EntityAction;
import org.bukkit.event.Event;

/** Represents a "generalized" {@link Event} that wraps another {@link Event}. */
public abstract class GeneralizingEvent extends ExtendedCancellable
    implements Cancellable, Physical, EntityAction {

  private final @Nullable Event cause;
  private boolean propagateCancel;

  public GeneralizingEvent(@Nullable Event cause) {
    super();
    this.cause = cause;
    this.propagateCancel = true;
  }

  @Nullable
  public Event getCause() {
    return cause;
  }

  @Override
  public World getWorld() {
    return getCause() instanceof Physical ? ((Physical) getCause()).getWorld() : null;
  }

  @Override
  public Entity getActor() {
    return getCause() instanceof EntityAction ? ((EntityAction) getCause()).getActor() : null;
  }

  @Override
  public void setCancelled(boolean cancel) {
    super.setCancelled(cancel);

    if (propagateCancel && getCause() instanceof Cancellable) {
      ((Cancellable) getCause()).setCancelled(cancel);
    }
  }

  /**
   * Whether cancelling the {@link GeneralizingEvent} will also cancel the {@link #getCause()}.
   *
   * @param yes Whether to propagate cancels.
   */
  public void setPropagateCancel(boolean yes) {
    propagateCancel = yes;
  }
}
