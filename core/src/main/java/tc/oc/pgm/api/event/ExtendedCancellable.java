package tc.oc.pgm.api.event;

import javax.annotation.Nullable;
import net.kyori.text.Component;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;

/** Extension of {@link Cancellable} to allow for {@link Component} error messages. */
public abstract class ExtendedCancellable extends Event implements Cancellable {

  private boolean cancelled;
  private @Nullable Component cancelMessage;

  protected ExtendedCancellable() {
    this(null);
  }

  protected ExtendedCancellable(@Nullable Component cancelMessage) {
    this.cancelMessage = cancelMessage;
  }

  @Override
  public boolean isCancelled() {
    return cancelled;
  }

  @Override
  public void setCancelled(boolean cancel) {
    cancelled = cancel;
    cancelMessage = null;
  }

  public void setCancelled(boolean cancel, Component message) {
    setCancelled(cancel);
    cancelMessage = message;
  }

  public @Nullable Component getCancelMessage() {
    return cancelMessage;
  }
}
