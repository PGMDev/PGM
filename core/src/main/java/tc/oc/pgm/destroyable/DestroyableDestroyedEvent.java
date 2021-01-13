package tc.oc.pgm.destroyable;

import org.bukkit.event.HandlerList;
import org.checkerframework.checker.nullness.qual.NonNull;
import tc.oc.pgm.api.match.Match;

/**
 * Called when a {@link Destroyable} is completely destroyed.
 *
 * <p>Event is called after the destroyable state has been updated.
 */
public class DestroyableDestroyedEvent extends DestroyableEvent {
  public DestroyableDestroyedEvent(@NonNull Match match, @NonNull Destroyable destroyable) {
    super(match, destroyable);
  }

  private static final HandlerList handlers = new HandlerList();

  @Override
  public HandlerList getHandlers() {
    return handlers;
  }

  public static HandlerList getHandlerList() {
    return handlers;
  }
}
