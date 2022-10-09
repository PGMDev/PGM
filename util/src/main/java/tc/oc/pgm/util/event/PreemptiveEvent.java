package tc.oc.pgm.util.event;

import net.kyori.adventure.text.Component;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;

/** A {@link Cancellable} event that can be preempted with a reason. */
public abstract class PreemptiveEvent extends Event implements Cancellable {
  private boolean cancelled;
  private @Nullable Component reason;

  protected PreemptiveEvent() {
    this(null);
  }

  protected PreemptiveEvent(final @Nullable Component reason) {
    this.reason = reason;
  }

  @Override
  public boolean isCancelled() {
    return this.cancelled;
  }

  @Override
  public void setCancelled(final boolean cancel) {
    this.cancelled = cancel;
    if (!cancel) this.reason = null;
  }

  /**
   * Cancels the event with a reason.
   *
   * @param reason the cancellation reason
   */
  public void setCancelled(final @Nullable Component reason) {
    this.setCancelled(true);
    this.reason = reason;
  }

  /**
   * Gets the cancellation reason.
   *
   * @return a reason or {@code null} if not cancelled
   */
  public @Nullable Component getCancellationReason() {
    return this.reason;
  }
}
