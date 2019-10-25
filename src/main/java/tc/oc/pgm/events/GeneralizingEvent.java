package tc.oc.pgm.events;

import javax.annotation.Nullable;
import org.bukkit.Physical;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.event.Cancellable;
import org.bukkit.event.EntityAction;
import org.bukkit.event.Event;

public abstract class GeneralizingEvent extends ExtendedCancellable
    implements Cancellable, Physical, EntityAction {

  protected boolean propagateCancel = true;
  @Nullable protected final Event cause;

  public GeneralizingEvent(@Nullable Event cause) {
    super();
    this.cause = cause;
  }

  @Nullable
  public Event getCause() {
    return this.cause;
  }

  @Override
  public World getWorld() {
    return cause instanceof Physical ? ((Physical) cause).getWorld() : null;
  }

  @Override
  public Entity getActor() {
    return cause instanceof EntityAction ? ((EntityAction) cause).getActor() : null;
  }

  @Override
  public void setCancelled(boolean cancel) {
    super.setCancelled(cancel);

    if (this.propagateCancel && this.cause instanceof Cancellable) {
      ((Cancellable) this.cause).setCancelled(cancel);
    }
  }

  /**
   * Set whether or not cancelling (or un-cancelling) this generalized event automatically cancels
   * its wrapped cause event. This is enabled by default.
   */
  public void setPropagateCancel(boolean yes) {
    this.propagateCancel = yes;
  }
}
