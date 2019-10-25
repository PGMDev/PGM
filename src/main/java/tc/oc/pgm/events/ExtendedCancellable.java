package tc.oc.pgm.events;

import javax.annotation.Nullable;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import tc.oc.component.Component;

/**
 * Extension of Bukkit's {@link Cancellable} to allow for custom error messages to be specified when
 * cancelling the event.
 */
public abstract class ExtendedCancellable extends Event implements Cancellable {

  protected boolean cancelled;
  protected @Nullable Component cancelMessage;

  protected ExtendedCancellable() {
    this(null);
  }

  protected ExtendedCancellable(@Nullable Component cancelMessage) {
    this.cancelMessage = cancelMessage;
  }

  @Override
  public boolean isCancelled() {
    return this.cancelled;
  }

  @Override
  public void setCancelled(boolean cancel) {
    this.cancelled = cancel;
    this.cancelMessage = null;
  }

  public void setCancelled(boolean cancel, Component message) {
    this.setCancelled(cancel);
    this.cancelMessage = message;
  }

  public @Nullable Component getCancelMessage() {
    return this.cancelMessage;
  }
}
